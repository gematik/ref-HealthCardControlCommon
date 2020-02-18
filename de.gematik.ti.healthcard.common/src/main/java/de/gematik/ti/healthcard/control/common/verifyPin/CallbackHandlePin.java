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

package de.gematik.ti.healthcard.control.common.verifyPin;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.healthcard.control.common.HealthcardCommonRuntimeException;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.openhealthcard.events.control.CommonEventTransmitter;
import de.gematik.ti.openhealthcard.events.response.callbacks.IPinNumberResponseListener;
import de.gematik.ti.openhealthcard.events.response.entities.PinNumber;

/**
 * implementation of {@link IPinNumberResponseListener} as {@link Callable}
 */
public class CallbackHandlePin implements IPinNumberResponseListener, Callable<ResultOperation<PinResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(CallbackHandlePin.class);

    private final ArrayBlockingQueue<ResultOperation<PinResult>> queue = new ArrayBlockingQueue<>(1);
    private final IHealthCard card;
    private ResultOperation<PinResult> pinResultResultOperation = null;

    public CallbackHandlePin(final IHealthCard card) {
        this.card = card;
    }

    /**
     * Get returned result in {@link #handlePinNumber(de.gematik.ti.openhealthcard.events.response.entities.PinNumber)}
     * @return
     */
    @Override
    public ResultOperation<PinResult> call() {
        try {
            pinResultResultOperation = queue.take();
            LOG.debug("call.take.queue.pinResultResultOperation: " + pinResultResultOperation);
        } catch (final InterruptedException e) {
            LOG.error(e.toString());
            Thread.currentThread().interrupt();
        }
        return pinResultResultOperation;
    }

    /**
     * Run verfication, deliver result back
     * @param pinNumber
     */
    @Override
    public void handlePinNumber(final PinNumber pinNumber) {
        try {
            if (card.getStatus().isValid()) {
                pinResultResultOperation = new PinVerifier(card)
                        .verifyPin(PinVerifier.stringToIntarray(pinNumber.getValue()), PinContainer.getPinConfiguration(card, pinNumber.getPinType()));
                abortRequest();
                queue.put(pinResultResultOperation);
                LOG.debug("handlePinNumber.put.queue.pinResultResultOperation: " + pinResultResultOperation);
            } else {
                LOG.error("Card is not valid");
                final RuntimeException exception = new RuntimeException("Card is not valid");
                CommonEventTransmitter.postError(exception, "PIN cannot be verified because card is invalid");
            }
        } catch (final Exception e) {
            LOG.error(e.toString());
            CommonEventTransmitter.postError(e, "PIN cannot be verified because error");
            throw new HealthcardCommonRuntimeException(e.toString());
        }
    }

    /**
     * abort the request action
     */
    @Override
    public void abortRequest() {
        pinResultResultOperation = pinResultResultOperation
                .map(pr -> {
                    if (!pr.isPinVerifiSuccess() && pr.getVerifyResultText() != null) {
                        final String failureText = pr.getVerifyResultText();
                        LOG.debug("failureText: " + failureText);
                        final Exception exception = new RuntimeException(failureText);
                        CommonEventTransmitter.postError(exception, failureText);
                    }
                    return pr;
                });
    }
}
