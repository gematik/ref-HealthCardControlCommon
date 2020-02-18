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

package de.gematik.ti.healthcard.control.common;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import de.gematik.ti.utils.codec.Hex;

/**
 * Test {@link Utilities}
 */
public class UtilitiesTest {

    private final byte[] byteArray = Hex
            .decode("044E2778F6AAEF54CB42865A3C30C753495AF4E53121400802D0AB1ACD665E9C774C2FAE1687E9DAA36C64570C909F93176F01EEAFCB45F9C08E49805F127D94EF");
    private final ECNamedCurveParameterSpec ecNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("BrainpoolP256r1");
    private final String expectedECPoint = "(4e2778f6aaef54cb42865a3c30c753495af4e53121400802d0ab1acd665e9c77,4c2fae1687e9daa36c64570c909f93176f01eeafcb45f9c08e49805f127d94ef,1,7d5a0975fc2c3057eef67530417affe7fb8055c126dc5c6ce94a4b44f330b5d9)";
    private final ECCurve.Fp curve = (ECCurve.Fp) ecNamedCurveParameterSpec.getCurve();

    @Test
    public void shouldCreateValidEcPointFromByteArray() {
        ECPoint point = Utilities.byteArrayToECPoint(byteArray, curve);
        Assert.assertEquals(expectedECPoint, point.toString());
    }

    @Test
    public void shouldEncodeAsn1KeyObject() throws IOException {
        final byte[] asn1InputArray = Hex.decode(
                "7C438341041B05278F276BD92E6B0EE3478BD3A93B03FE8E4C35556F0D6C13C89C504F91C065E85C1D289B306F61BE2CECCED4E7532BF0925A4907F246DF7A69C8D69ED24F");
        final byte[] expectedKeyArray = Hex
                .decode("041B05278F276BD92E6B0EE3478BD3A93B03FE8E4C35556F0D6C13C89C504F91C065E85C1D289B306F61BE2CECCED4E7532BF0925A4907F246DF7A69C8D69ED24F");
        byte[] keyArray = Utilities.getKeyObjectEncoded(asn1InputArray);
        Assert.assertEquals(Hex.encodeHexString(expectedKeyArray), Hex.encodeHexString(keyArray));
    }
}
