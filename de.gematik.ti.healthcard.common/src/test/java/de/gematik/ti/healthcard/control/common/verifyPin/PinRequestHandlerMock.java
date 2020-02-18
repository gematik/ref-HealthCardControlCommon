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

import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.openhealthcard.events.message.AbstractOpenHealthCardEvent;
import de.gematik.ti.openhealthcard.events.message.ErrorEvent;
import de.gematik.ti.openhealthcard.events.request.RequestPinNumberEvent;
import de.gematik.ti.openhealthcard.events.response.entities.PinNumber;

/**
 * A Mock of OpenHealthcardApp that communicate with @{@link PinVerifier} use @{@link EventBus}
 */
public class PinRequestHandlerMock {
    private static final Logger LOG = LoggerFactory.getLogger(PinRequestHandlerMock.class);

    private String[] getPinStatusRequestResponse;
    private String[] verifyRequestResponse;
    private String pinInput = "123456";
    private String errorText;

    public PinRequestHandlerMock() {
    }

    /**
     *
     * @param getPinStatusRequestResponse
     * @param verifyRequestResponse
     */
    public PinRequestHandlerMock(final String[] getPinStatusRequestResponse, final String[] verifyRequestResponse) {
        this.getPinStatusRequestResponse = getPinStatusRequestResponse;
        this.verifyRequestResponse = verifyRequestResponse;
    }

    /**
     * handle coming {@link RequestPinNumberEvent}
     * @param event
     */
    @Subscribe
    public void handlePinNumber(final RequestPinNumberEvent event) {
        LOG.debug("event " + event);
        // get input from user
        final String pin = getPinInput();
        final String pinType = event.getPinType();
        LOG.debug("pinType: " + pinType);

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        event.getResponseListener().handlePinNumber(new PinNumber(pinType, pin));
    }

    private String getPinInput() {
        return pinInput;
    }

    /**
     * set a new PIN  
     * @param newPin
     */
    public void setPinInput(final String newPin) {
        pinInput = newPin;
    }

    /**
     * handle a coming {@link AbstractOpenHealthCardEvent}
     * @param event
     */
    @Subscribe
    public void handleMessagesFromEventBus(final AbstractOpenHealthCardEvent event) {
        if (event instanceof ErrorEvent) {
            LOG.error(event.getSourceClass() + ": " + event.getMessage());
            errorText = event.getMessage();
            ((ErrorEvent) event).getThrowable().printStackTrace();
        } else {
            LOG.info(event.getSourceClass() + ": " + event.getMessage());
        }
    }

    /**
     * get error text
     * @return
     */
    public String getErrorText() {
        return errorText;
    }
}
