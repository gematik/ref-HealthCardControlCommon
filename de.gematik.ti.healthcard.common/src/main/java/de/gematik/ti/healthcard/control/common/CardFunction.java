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

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.cardreader.provider.api.card.ICard;
import de.gematik.ti.healthcardaccess.HealthCard;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.IHealthCardType;
import de.gematik.ti.healthcardaccess.healthcards.Egk2;
import de.gematik.ti.healthcardaccess.healthcards.Egk21;
import de.gematik.ti.healthcardaccess.healthcards.HealthCardStatusValid;

/**
 * supply methodes for repeated use of {@link IHealthCard}
 */
public final class CardFunction {
    private static final Logger LOG = LoggerFactory.getLogger(CardFunction.class);

    private CardFunction() {
    }

    /**
     * Check if cardHc is valid and it's type is of 'clazz'
     * @param cardHc
     * @return
     */
    public static <T extends IHealthCardType> boolean isCardType(final IHealthCard cardHc, final Class<T> clazz) {
        return Stream.of(cardHc.getStatus()).filter(s -> s.isValid())
                .map(s -> ((HealthCardStatusValid) s).getHealthCardType())
                .peek(d -> LOG.trace("isCardType: " + d + ", expected: " + clazz))
                .filter(t -> t.getClass().equals(clazz)).findAny().isPresent();

    }

    /**
     * create healthCard with Type-Setting, connect card and hcType, a check with {@link #isCardType(IHealthCard, Class)} makes no sense.
     * @param card
     * @param hcType
     * @return
     */
    public static <T extends IHealthCardType> IHealthCard createHealthCardWithTypeSetting(final ICard card, final T hcType)
            throws HealthcardCommonRuntimeException {
        final Stream<? extends IHealthCard> r = Stream.of(card).map(HealthCard::new).peek(d -> LOG.debug("cardType: " + d + ", expected: " + hcType))
                .flatMap(hc -> {
                    hc.setHealthCardType(hcType);
                    return Stream.of(hc);
                });
        final Optional<? extends IHealthCard> optional = r.findAny();
        if (optional.isPresent()) {
            final IHealthCard healthCard = optional.get();
            return healthCard;
        }
        throw new HealthcardCommonRuntimeException("no healthCard available for type " + hcType);
    }

    /**
     * Check cardHc is eGK no matter of its generatiorn
     * @param cardHc
     * @return
     */
    public static boolean isEgk(final IHealthCard cardHc) {
        return CardFunction.isCardType(cardHc, Egk2.class) || CardFunction.isCardType(cardHc, Egk21.class);
    }

    /**
     * get className of a cardType for healthCard
     * @param cardHc
     * @return
     */
    public static String getCardType(final IHealthCard cardHc) {
        Optional<IHealthCardType> optional = Stream.of(cardHc.getStatus()).filter(s -> s.isValid())
                .map(s -> ((HealthCardStatusValid) s).getHealthCardType()).findAny();
        if (optional.isPresent()) {
            return optional.get().getClass().getName();
        }
        throw new HealthcardCommonRuntimeException("no healthCard available for card " + cardHc);
    }

    /**
     * check if a healthcard valid or not
     * @param cardHc
     * @return
     */
    public static boolean isCardValid(final IHealthCard cardHc) {
        return cardHc.getStatus().isValid();
    }
}
