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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.*;

class PaceInfo {

    private static final Logger LOG = LoggerFactory.getLogger(PaceInfo.class);
    private static final int PARAMETER256 = 13;
    private static final int PARAMETER384 = 16;
    private static final int PARAMETER512 = 17;

    private final byte[] cardAccess;
    private byte[] paceInfoProtocolBytes;
    private ASN1ObjectIdentifier protocol;
    private int parameterID;

    /**
     * constructor with parameter
     * @param cardAccess byte array from extracted card access
     * @throws IOException if an error occurred
     */
    PaceInfo(final byte[] cardAccess) throws IOException {

        this.cardAccess = cardAccess;
        if (cardAccess != null) {
            extractProtocol();
        } else {
            LOG.error("CardAccess is null");
        }
    }

    private void extractProtocol() throws IOException {
        if (cardAccess != null) {
            try (final ASN1InputStream asn1InputStream = new ASN1InputStream(cardAccess)) {
                final DLSet app = (DLSet) asn1InputStream.readObject();
                final ASN1Sequence seq = (ASN1Sequence) app.getObjectAt(0);

                protocol = (ASN1ObjectIdentifier) seq.getObjectAt(0);
                parameterID = ((ASN1Integer) seq.getObjectAt(2)).getValue().intValue();

                if (protocol == null) {
                    LOG.error("Protocol == null");
                } else {
                    paceInfoProtocolBytes = new byte[protocol.getEncoded().length - 2];
                    System.arraycopy(protocol.getEncoded(), 2, paceInfoProtocolBytes, 0, paceInfoProtocolBytes.length);
                }
            }
        }
    }

    /**
     * Returns PACE info protocol bytes
     * @return byte array with protocol bytes
     */
    byte[] getPaceInfoProtocolBytes() {
        return paceInfoProtocolBytes;
    }

    String getParameterIDString() {
        final String parameter;
        switch (parameterID) {
            case PARAMETER256:
                parameter = "BrainpoolP256r1";
                break;
            case PARAMETER384:
                parameter = "BrainpoolP384r1";
                break;
            case PARAMETER512:
                parameter = "BrainpoolP512r1";
                break;
            default:
                parameter = "";
        }
        return parameter;
    }

    /**
     * Returns PACE info protocol ID
     * @return PACE info protocol ID
     */
    String getProtocolID() {
        return protocol.getId();
    }
}
