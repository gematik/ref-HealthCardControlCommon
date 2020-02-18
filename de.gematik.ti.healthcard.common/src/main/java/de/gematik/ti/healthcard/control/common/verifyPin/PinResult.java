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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The result of information about PIN verification
 */
public class PinResult {
    private static final Logger LOG = LoggerFactory.getLogger(PinResult.class);
    private boolean pinVerifiSuccess;
    private int numberRemain = -1;
    private String verifyResultText = null;

    /**
     * constructor with parameter
     * @param status
     */
    public PinResult(final String status) {
        LOG.debug("status: " + status);
        verifyResultText = status;
        PinState pinState = PinState.valueOf(status);
        if (status.equalsIgnoreCase("SUCCESS")) {
            pinState = PinState.NO_ERROR;
        }
        switch (pinState) {
            case NO_ERROR:
                pinVerifiSuccess = true;
                break;
            case RETRY_COUNTER_COUNT_00:
                setNumberRemain(0);
                break;
            case RETRY_COUNTER_COUNT_01:
                setNumberRemain(1);
                pinVerifiSuccess = true;
                break;
            case RETRY_COUNTER_COUNT_02:
                setNumberRemain(2);
                pinVerifiSuccess = true;
                break;
            case RETRY_COUNTER_COUNT_03:
                setNumberRemain(3);
                pinVerifiSuccess = true;
                break;
            case TRANSPORT_STATUS_TRANSPORT_PIN:
            case TRANSPORT_STATUS_EMPTY_PIN:
            case PASSWORD_DISABLED:
            case SECURITY_STATUS_NOT_SATISFIED:
            case PASSWORD_NOT_FOUND:
                pinVerifiSuccess = false;
                break;
            default:
                LOG.error("status +" + status + "' is not defined in PinResult.");
                break;
        }
    }

    /**
     * get nubmerRemain
     * @return
     */
    public int getNumberRemain() {
        return numberRemain;
    }

    /**
     * set nubmerRemain
     * @param numberRemain
     */
    public void setNumberRemain(final int numberRemain) {
        this.numberRemain = numberRemain;
    }

    /**
     * get result of pin verification
     * @return
     */
    public boolean isPinVerifiSuccess() {
        return pinVerifiSuccess;
    }

    /**
     *  set result of pin verification
     * @param pinVerifiSuccess
     * @return
     */
    public PinResult setPinVerifiSuccess(final boolean pinVerifiSuccess) {
        this.pinVerifiSuccess = pinVerifiSuccess;
        return this;
    }

    /**
     * get result of verification in detail
     * @return
     */
    public String getVerifyResultText() {
        return verifyResultText;
    }

    /**
     * states of PIN
     */
    public enum PinState {
        TRANSPORT_STATUS_TRANSPORT_PIN,
        TRANSPORT_STATUS_EMPTY_PIN,
        PASSWORD_DISABLED,
        RETRY_COUNTER_COUNT_00,
        RETRY_COUNTER_COUNT_01,
        RETRY_COUNTER_COUNT_02,
        RETRY_COUNTER_COUNT_03,
        NO_ERROR,
        SECURITY_STATUS_NOT_SATISFIED,
        PASSWORD_NOT_FOUND
    }
}
