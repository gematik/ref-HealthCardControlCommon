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
import java.math.BigInteger;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.DERApplicationSpecific;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import de.gematik.ti.utils.primitives.Bytes;

/**
 * some utilities for usage
 */
public final class Utilities {

    private static final int UNCOMPRESSECPOINTVALUE = 0x04;

    private Utilities() {
    }

    /**
     * Decodes an ECPoint from byte array. Prime field p is taken from the passed curve
     * The first byte must contain the value 0x04 (uncompressed point).
     *
     * @param byteArray Byte array of the form {0x04 || x-bytes [] || y byte []}
     * @param curve     The curve on which the point should lie.
     * @return EC point generated from input data
     */
    public static ECPoint byteArrayToECPoint(final byte[] byteArray, final ECCurve curve) {
        final byte[] x = new byte[(byteArray.length - 1) / 2];
        final byte[] y = new byte[(byteArray.length - 1) / 2];
        if (byteArray[0] != (byte) UNCOMPRESSECPOINTVALUE) {
            throw new IllegalArgumentException("Found no uncompressed point!");
        } else {
            System.arraycopy(byteArray, 1, x, 0, (byteArray.length - 1) / 2);
            System.arraycopy(byteArray, 1 + ((byteArray.length - 1) / 2), y, 0,
                    (byteArray.length - 1) / 2);
            return curve.createPoint(new BigInteger(1, x), new BigInteger(1, y));
        }
    }

    /**
     * Encodes an ASN1 KeyObject
     *
     * @param asn1Input ASN1 Stream of the form [Application]Sequence{}
     * @return The encoded ByteArray
     * @throws IOException if an error occurred
     */
    public static byte[] getKeyObjectEncoded(final byte[] asn1Input) throws IOException {
        final byte[] key;
        try (final ASN1InputStream asn1InputStream = new ASN1InputStream(asn1Input)) {
            final DERApplicationSpecific seq = (DERApplicationSpecific) asn1InputStream.readObject();
            final ASN1Object seqObj = seq.getObject();
            key = Bytes.copyByteArray(seqObj.getEncoded(), 2, seqObj.getEncoded().length - 2);
        }
        return key;
    }
}
