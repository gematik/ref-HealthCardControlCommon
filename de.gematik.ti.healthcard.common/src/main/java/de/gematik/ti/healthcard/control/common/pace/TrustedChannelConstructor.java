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

package de.gematik.ti.healthcard.control.common.pace;

import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.healthcardaccess.HealthCard;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.exceptions.runtime.BasicChannelException;
import de.gematik.ti.healthcardaccess.operation.Subscriber;
import de.gematik.ti.openhealthcard.events.control.RequestTransmitter;
import de.gematik.ti.openhealthcard.events.request.RequestPaceKeyEvent;
import de.gematik.ti.openhealthcard.events.response.entities.CardAccessNumber;
import de.gematik.ti.openhealthcard.events.response.entities.PaceKey;

/**
 * Handles the RequestPaceKey Events and trigger the negotiation steps
 */
public class TrustedChannelConstructor extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(TrustedChannelConstructor.class);
    private static final String TAG = "TrustedChannelConstructor: ";
    private static final int TIMEOUT = 30;
    private final RequestPaceKeyEvent requestPaceKeyEvent;

    TrustedChannelConstructor(final RequestPaceKeyEvent requestPaceKeyEvent) {
        this.requestPaceKeyEvent = requestPaceKeyEvent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        super.run();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CardAccessNumberRequester cardAccessNumberRequester = new CardAccessNumberRequester();
        new RequestTransmitter().cardAccessNumber().request(cardAccessNumberRequester);
        final Future<CardAccessNumber> future = executor.submit(cardAccessNumberRequester);
        executor.shutdown();
        CardAccessNumber cardAccessNumber = null;
        try {
            cardAccessNumber = future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (final TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
        }

        if (cardAccessNumber != null) {
            try {
                final IHealthCard healthCard = new HealthCard(requestPaceKeyEvent.getCard());

                new TrustedChannelPaceKeyExchange(healthCard, cardAccessNumber.getValue()).negotiatePaceKey().subscribe(
                        new Subscriber<PaceKey>() {
                            @Override
                            public void onSuccess(final PaceKey paceKey) {
                                requestPaceKeyEvent.getResponseListener()
                                        .handlePaceKey(paceKey);
                            }

                            @Override
                            public void onError(final Throwable t) throws RuntimeException {
                                LOG.error(TAG, "PaceKey negotiation failed! " + t.getMessage());
                            }
                        });

            } catch (final BasicChannelException basic) {
                LOG.error(TAG, "PaceKey negotiation failed! Broken Card-Channel" + basic);
            }
        }
    }
}
