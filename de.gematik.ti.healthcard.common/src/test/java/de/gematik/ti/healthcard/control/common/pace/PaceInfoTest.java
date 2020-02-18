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

import org.junit.Assert;
import org.junit.Test;

import de.gematik.ti.utils.codec.Hex;

/**
 * Test {@link PaceInfo}
 */
public class PaceInfoTest {

    @Test
    public void testPaceInfoExtraction() throws IOException {

        byte[] cardAccessBytes = Hex.decode("31143012060A04007F0007020204020202010202010D");

        String expectedProtocolId = "0.4.0.127.0.7.2.2.4.2.2";
        byte[] expectedPaceInfoProtocolBytes = Hex.decode("04007F00070202040202");
        String expectedParameterIDString = "BrainpoolP256r1";

        PaceInfo paceInfo = new PaceInfo(cardAccessBytes);
        Assert.assertNotNull(paceInfo);

        String protocolId = paceInfo.getProtocolID();
        Assert.assertEquals(expectedProtocolId, protocolId);

        byte[] paceInfoProtocolBytes = paceInfo.getPaceInfoProtocolBytes();
        Assert.assertEquals(Hex.encodeHexString(expectedPaceInfoProtocolBytes), Hex.encodeHexString(paceInfoProtocolBytes));

        String parameterIDString = paceInfo.getParameterIDString();
        Assert.assertEquals(expectedParameterIDString, parameterIDString);
    }
}
