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

/**
 * enumeration of error occured during PIN verification
 */
public enum PinVerifyError {
    timeout(1002, "Zeitüberschreitung (Timeout)"),
    accessErr(1007, "Fehler beim Zugriff auf die Karte"),
    interruption(1013, "Abbruch durch den Benutzer"), // cancel
    blocking(1061, "PIN blockiert");

    private final String errorMsg;
    private final int errorIndex;

    /**
     * constructor with parameter
     * @param errorIndex
     * @param errorMsg
     */
    PinVerifyError(final int errorIndex, final String errorMsg) {
        this.errorIndex = errorIndex;
        this.errorMsg = errorMsg;
    }

    /**
     * error text
     * @return
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public String toString() {
        return getErrorIndex() + ": " + getErrorMsg();
    }
}
