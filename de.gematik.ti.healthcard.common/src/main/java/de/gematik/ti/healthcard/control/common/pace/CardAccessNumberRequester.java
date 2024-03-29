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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.openhealthcard.events.response.callbacks.ICardAccessNumberResponseListener;
import de.gematik.ti.openhealthcard.events.response.entities.CardAccessNumber;

/**
 * implementation of {@link ICardAccessNumberResponseListener} as {@link Callable}
 */
public class CardAccessNumberRequester implements Callable<CardAccessNumber>, ICardAccessNumberResponseListener {
    private static final Logger LOG = LoggerFactory.getLogger(CardAccessNumberRequester.class);
    private static final String TAG = "CardAccessNumberRequester: ";

    private final ArrayBlockingQueue<CardAccessNumber> queue = new ArrayBlockingQueue<>(1);

    /**
     * get instance if one exists
     * @return
     * @throws Exception
     */
    @Override
    public CardAccessNumber call() throws Exception {
        return queue.take();
    }

    /**
     * save coming {@link CardAccessNumber} in {@link #queue}
     * @param cardAccessNumber
     */
    @Override
    public void handleCan(final CardAccessNumber cardAccessNumber) {
        try {
            queue.put(cardAccessNumber);
        } catch (final InterruptedException e) {
            LOG.debug(TAG, "Error by add cardAccessNumber to Queue", e);
            Thread.currentThread().interrupt();
        }
    }
}
