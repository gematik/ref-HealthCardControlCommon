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
 * supplier of applicationId, in which the password object is, and the passwordId
 */
public class PinConfiguration {

    private final String[] appIds;
    private final int pwid;

    /**
     * constructor with parameter
     * @param appids
     * @param pwid
     */
    public PinConfiguration(final String[] appids, final int pwid) {
        appIds = appids;
        this.pwid = pwid;
    }

    /**
     * get the applicationId of whole DF
     * @return
     */
    public String[] getAppIds() {
        return appIds;
    }

    /**
     * get ID of password object 
     * @return
     */
    public int getPwid() {
        return pwid;
    }
}
