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

import org.junit.Assert;
import org.junit.Test;

import de.gematik.ti.utils.codec.Hex;

/**
 * Test {@link KeyDerivationFunction}
 */
public class KeyDerivationFunctionTest {

    private final byte[] secretK = Hex.decode("2ECA74E72CD6C1E0DA235093569984987C34A9F4D34E4E60FB0AD87B983CDC62");

    @Test
    public void shouldReturnValidAES128KeyModeEnc() {
        byte[] validAes128Key = Hex.decode("AB5541629D18E5F33EE2B13DBDCDBE84");
        byte[] aes128Key = KeyDerivationFunction.getAES128Key(secretK, KeyDerivationFunction.Mode.ENC);
        Assert.assertEquals(Hex.encodeHexString(aes128Key), Hex.encodeHexString(validAes128Key));
    }

    @Test
    public void shouldReturnValidAES128KeyModeMac() {
        byte[] validAes128Key = Hex.decode("E13D3757C7D9073794A3D7CA94B22D30");
        byte[] aes128Key = KeyDerivationFunction.getAES128Key(secretK, KeyDerivationFunction.Mode.MAC);
        Assert.assertEquals(Hex.encodeHexString(aes128Key), Hex.encodeHexString(validAes128Key));
    }

    @Test
    public void shouldReturnValidAES128KeyModePassword() {
        byte[] validAes128Key = Hex.decode("74C1F5E712B53BAAA3B02B182E0961B9");
        byte[] aes128Key = KeyDerivationFunction.getAES128Key(secretK, KeyDerivationFunction.Mode.PASSWORD);
        Assert.assertEquals(Hex.encodeHexString(aes128Key), Hex.encodeHexString(validAes128Key));
    }
}
