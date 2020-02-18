/*
 * Copyright (c) 2020 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.ti.healthcard.control.common;

import java.util.HashMap;
import java.util.Map;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cardfilesystem.Egk2FileSystem;
import cardfilesystem.Hba2FileSystem;
import cardfilesystem.Smcb2FileSystem;
import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.card.CardException;
import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.cardreader.provider.api.events.CardReaderDisconnectedEvent;
import de.gematik.ti.cardreader.provider.api.events.card.CardAbsentEvent;
import de.gematik.ti.cardreader.provider.api.events.card.CardPresentEvent;
import de.gematik.ti.healthcard.control.common.event.absent.*;
import de.gematik.ti.healthcard.control.common.event.present.*;
import de.gematik.ti.healthcardaccess.AbstractHealthCardCommand;
import de.gematik.ti.healthcardaccess.HealthCard;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.IHealthCardType;
import de.gematik.ti.healthcardaccess.cardobjects.ApplicationIdentifier;
import de.gematik.ti.healthcardaccess.cardobjects.FileControlParameter;
import de.gematik.ti.healthcardaccess.cardobjects.ShortFileIdentifier;
import de.gematik.ti.healthcardaccess.commands.ReadCommand;
import de.gematik.ti.healthcardaccess.commands.SelectCommand;
import de.gematik.ti.healthcardaccess.entities.Version2;
import de.gematik.ti.healthcardaccess.healthcards.*;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.healthcardaccess.operation.Subscriber;
import de.gematik.ti.healthcardaccess.result.Response;
import de.gematik.ti.utils.codec.Hex;
import de.gematik.ti.utils.tuple.Pair;

/**
 * detector of available ehealth card
 */
public final class CardDetector {
    private static final Logger LOG = LoggerFactory.getLogger(CardDetector.class);
    private static final String TAG = "CardDetector: ";
    private static CardDetector instance;
    private final Map<ICardReader, IHealthCard> healthCardMap = new HashMap<>();

    private enum CARD_TYPE {
        EGK,
        HBA,
        SMCB,
        UNKNOWN
    }

    private CardDetector() {
    }

    /**
     * Get the singleton instance
     * @return CardDetector instance
     */
    public static CardDetector getInstance() {
        if (instance == null) {
            instance = new CardDetector();
        }
        return instance;
    }

    /**
     * Start the detection of card present and absent events such as card reader disconnection events
     */
    public static void startDetection() {
        getInstance().register();
    }

    /**
     * Stop the detection of card present and absent events such as card reader disconnection events
     */
    public static void stopDetection() {
        getInstance().unregister();
    }

    private void register() {
        EventBus.getDefault().register(this);
        LOG.debug(TAG + "registered");
    }

    private void unregister() {
        EventBus.getDefault().unregister(this);
        LOG.debug(TAG + "unregistered");

    }

    /**
     * Method that handles the CardPresentEvents and trigger the analysis of present cards
     * @param cardPresentEvent Event for present Card
     */
    @Subscribe
    public void handleCardPresentEvents(final CardPresentEvent cardPresentEvent) {
        final ICardReader cardReader = cardPresentEvent.getCardReader();
        LOG.debug(TAG + "cardPresentEvent at " + cardReader.getName());

        ICard card = null;
        try {
            card = cardReader.connect();
        } catch (final CardException e) {
            LOG.debug("Card Reader can't connect to Card. Wrong Card present?", e);
        }

        if (card != null) {
            createHealthCardWithTypeAndSendEvent(cardReader, card);
        }
    }

    private void createHealthCardWithTypeAndSendEvent(final ICardReader cardReader, final ICard card) {
        final HealthCard healthCard = new HealthCard(card);
        final AbstractHealthCardCommand selectRootCommand = new SelectCommand(false, true);
        // test nfc:
        final int sfid = getSfid(healthCard);
        final ShortFileIdentifier sfi = new ShortFileIdentifier(sfid);
        final AbstractHealthCardCommand readVersion2 = new ReadCommand(sfi, 0);

        selectRootCommand.executeOn(healthCard)
                .validate(Response.ResponseStatus.SUCCESS::validateResult)
                .map(Response::getResponseData)
                .map(FileControlParameter::new)
                .map(FileControlParameter::getApplicationIdentifier)
                .map(ApplicationIdentifier::new)
                .map(this::extractCardType)
                .flatMap(card_type -> readVersion2.executeOn(healthCard)
                        .map(Response::getResponseData)
                        .map(Version2::fromArray)
                        .map(CardGenerationExtractor::getCardGeneration)
                        .flatMap(cardGeneration -> ResultOperation.unitRo(new Pair<>(card_type, cardGeneration))))
                .subscribe(new Subscriber<Pair<CARD_TYPE, CardGeneration>>() {
                    @Override
                    public void onSuccess(final Pair<CARD_TYPE, CardGeneration> value) {
                        sendEvent(cardReader, healthCard, value.left, value.right);
                    }

                    @Override
                    public void onError(final Throwable t) throws RuntimeException {
                        LOG.error("Error when reading CardType or Version", t.getMessage());
                    }
                });
    }

    /**
     * In this phase is cardType still unknown. Version2.SDID is for all cardType '0x11'
     * @param healthCard
     * @return
     */
    private int getSfid(final HealthCard healthCard) {
        final int commonSfid = Egk2FileSystem.EF.Version2.SFID;
        return commonSfid;
    }

    private CARD_TYPE extractCardType(final ApplicationIdentifier applicationIdentifier) {
        switch (Hex.encodeHexString(applicationIdentifier.getAid())) {
            case Egk2FileSystem.AID:
                return CARD_TYPE.EGK;
            case Hba2FileSystem.AID:
                return CARD_TYPE.HBA;
            case Smcb2FileSystem.AID:
                return CARD_TYPE.SMCB;
            default:
                return CARD_TYPE.UNKNOWN;
        }
    }

    private boolean sendEvent(final ICardReader cardReader, final HealthCard healthCard, final CARD_TYPE type,
            final CardGeneration cardGeneration) {
        switch (type) {
            case EGK:
                healthCard.setHealthCardType(getHealthCardTypeForEgk(cardGeneration));
                break;
            case HBA:
                healthCard.setHealthCardType(getHealthCardTypeHba(cardGeneration));
                break;
            case SMCB:
                healthCard.setHealthCardType(getHealthCardTypeSmcb(cardGeneration));
                break;
            default:
                healthCard.setHealthCardType(new Unknown());
        }

        EventBus.getDefault().post(createPresentEventForHealthCardType(cardReader, healthCard));
        LOG.debug(TAG + "sendCardPresentEvent at " + cardReader.getName() + " " + healthCard.getStatus());
        healthCardMap.put(cardReader, healthCard);
        return true;
    }

    private AbstractHealthCardPresentEvent createPresentEventForHealthCardType(final ICardReader cardReader, final IHealthCard healthCard) {
        if (healthCard.getStatus().isValid()) {
            final IHealthCardType healthCardType = ((HealthCardStatusValid) healthCard.getStatus()).getHealthCardType();
            if (healthCardType instanceof Egk2) {
                return new Egk2HealthCardPresentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Egk21) {
                return new Egk21HealthCardPresentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Hba2) {
                return new Hba2HealthCardPresentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Hba21) {
                return new Hba21HealthCardPresentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Smcb2) {
                return new Smcb2HealthCardPresentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Smcb21) {
                return new Smcb21HealthCardPresentEvent(cardReader, healthCard);
            }
        }
        return new UnknownCardPresentEvent(cardReader, healthCard);
    }

    private IHealthCardType getHealthCardTypeSmcb(final CardGeneration cardGeneration) {
        switch (cardGeneration) {
            case G2:
                return new Smcb2();
            case G2_1:
                return new Smcb21();
            default:
                return null;
        }
    }

    private IHealthCardType getHealthCardTypeHba(final CardGeneration cardGeneration) {
        switch (cardGeneration) {
            case G2:
                return new Hba2();
            case G2_1:
                return new Hba21();
            default:
                return null;
        }
    }

    private IHealthCardType getHealthCardTypeForEgk(final CardGeneration cardGeneration) {
        switch (cardGeneration) {
            case G2:
                return new Egk2();
            case G2_1:
                return new Egk21();
            default:
                return null;
        }
    }

    /**
     * Handle the card absent events and inform event bus subscriber about absent health card if card before known as health card
     * @param cardAbsentEvent Event for absent card
     */
    @Subscribe
    public void handleCardAbsentEvents(final CardAbsentEvent cardAbsentEvent) {
        LOG.debug(TAG + "handleCardAbsentEvents at " + cardAbsentEvent.getCardReader().getName());
        sendCardAbsentEvent(cardAbsentEvent.getCardReader());

    }

    private void sendCardAbsentEvent(final ICardReader cardReader) {
        LOG.debug(TAG + "sendCardAbsentEvent at " + cardReader.getName());
        if (healthCardMap.containsKey(cardReader)) {
            final IHealthCard iHealthCard = healthCardMap.remove(cardReader);
            EventBus.getDefault().post(createAbsentEventForHealthCardType(cardReader, iHealthCard));
        }
    }

    /**
     * Handle the card reader disconnection events and inform event bus subscriber about absent health card if card before known as health card and the card
     * is present at card reader disconnection
     * @param cardReaderDisconnectedEvent Event for card reader disconnection
     */
    @Subscribe
    public void handleCardReaderDisconnectedEvents(final CardReaderDisconnectedEvent cardReaderDisconnectedEvent) {
        LOG.debug(TAG + "handleCardReaderDisconnectedEvents at " + cardReaderDisconnectedEvent.getCardReader().getName());
        sendCardAbsentEvent(cardReaderDisconnectedEvent.getCardReader());

    }

    private AbstractHealthCardAbsentEvent createAbsentEventForHealthCardType(final ICardReader cardReader, final IHealthCard healthCard) {
        if (healthCard.getStatus().isValid()) {
            final IHealthCardType healthCardType = ((HealthCardStatusValid) healthCard.getStatus()).getHealthCardType();
            if (healthCardType instanceof Egk2) {
                return new Egk2HealthCardAbsentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Egk21) {
                return new Egk21HealthCardAbsentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Hba2) {
                return new Hba2HealthCardAbsentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Hba21) {
                return new Hba21HealthCardAbsentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Smcb2) {
                return new Smcb2HealthCardAbsentEvent(cardReader, healthCard);
            }
            if (healthCardType instanceof Smcb21) {
                return new Smcb21HealthCardAbsentEvent(cardReader, healthCard);
            }
        }
        return new UnknownCardAbsentEvent(cardReader, healthCard);
    }
}
