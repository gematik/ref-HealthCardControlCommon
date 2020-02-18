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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.healthcard.control.common.CardFunction;
import de.gematik.ti.healthcard.control.common.HealthcardCommonRuntimeException;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.openhealthcard.events.control.CommonEventTransmitter;
import de.gematik.ti.openhealthcard.events.control.RequestTransmitterPinNumber;
import de.gematik.ti.openhealthcard.events.message.AbstractOpenHealthCardEvent;
import de.gematik.ti.openhealthcard.events.message.CancelEvent;

/**
 * launcher of pin verification action
 */
public class PinVerfiyLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(PinVerfiyLauncher.class);
    private static final long TIMEOUT_SECONDS = 30;
    private final IHealthCard card;
    private CompletableFuture<ResultOperation<PinResult>> completableFuture;

    /**
     * constructor with parameter
     * @param card
     */
    public PinVerfiyLauncher(final IHealthCard card) {
        this.card = card;
    }

    /**
     * Performs a PIN entry to a card
     * The operation causes a prompt for entering the PinReference designated PIN - regardless of whether the PIN previously successful
     * entered and checked. The card reader transmits the PIN for verification to the chosen card. The test result provides information
     * about the success or failure of the PIN verification and, if necessary, the number of remaining PIN entry attempts.
     *
     * @return
     */
    public ResultOperation<PinResult> verifyPin(final String pinType) {
        EventBus.getDefault().register(this);
        LOG.debug("pinType: " + pinType);
        if (!CardFunction.isCardValid(card)) {
            throw new HealthcardCommonRuntimeException("card '" + card + "' is invalid");
        }

        final CallbackHandlePin callback = new CallbackHandlePin(card);
        final Runnable task = () -> {
            final RequestTransmitterPinNumber requestTransmitterPinNumber = new RequestTransmitterPinNumber();
            requestTransmitterPinNumber.request(callback, pinType);
        };
        new Thread(task).start();

        try {
            completableFuture = CompletableFuture.supplyAsync(() -> callback.call());
            return completableFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException | ExecutionException e) {
            throw new HealthcardCommonRuntimeException(e.toString());
        } catch (final TimeoutException e) {
            CommonEventTransmitter.postWarn(PinVerifyError.timeout.toString());
            throw new HealthcardCommonRuntimeException(PinVerifyError.timeout.toString());
        } finally {
            EventBus.getDefault().unregister(this);
        }
    }

    /**
     * handle a coming event for cancelation
     * @param event
     */
    @Subscribe
    public void subscribeCancleEvent(final AbstractOpenHealthCardEvent event) {
        if (event instanceof CancelEvent) {
            LOG.debug("cancelEvent received: " + event);
            if (completableFuture != null) {
                final boolean cancel = completableFuture.cancel(true);
                if (cancel) {
                    CommonEventTransmitter.postInfo(PinVerifyError.interruption.toString());
                    LOG.info(PinVerifyError.interruption.toString());
                }
            }
        }
    }

}
