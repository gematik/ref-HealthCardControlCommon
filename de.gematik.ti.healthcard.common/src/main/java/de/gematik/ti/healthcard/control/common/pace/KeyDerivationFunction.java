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

import java.util.Arrays;

import org.spongycastle.crypto.digests.SHA1Digest;

/*
 * This class provides functionality to derive AES-128 keys.
 */
final class KeyDerivationFunction {

    private static final int CHECKSUMLENGTH = 20;
    private static final int AES128LENGTH = 16;
    private static final int OFFSETLENGTH = 4;
    private static final int ENCLASTBYTE = 1;
    private static final int MACLASTBYTE = 2;
    private static final int PASSWORDLASTBYTE = 3;

    /**
     * state of key
     */
    public enum Mode {
        ENC, // key for encryption/decryption
        MAC, // key for MAC
        PASSWORD // encryption keys from a password
    }

    private KeyDerivationFunction() {
    }

    /**
     * derive AES-128 key
     *
     * @param sharedSecretK byte array with shared secret value.
     * @param mode key derivation for ENC, MAC or derivation from password
     * @return byte array with AES-128 key
     */
    static byte[] getAES128Key(final byte[] sharedSecretK, final Mode mode) {
        final byte[] checksum = new byte[CHECKSUMLENGTH];
        final byte[] data = replaceLastKeyByte(sharedSecretK, mode);
        final SHA1Digest sha1 = new SHA1Digest();
        sha1.update(data, 0, data.length);
        sha1.doFinal(checksum, 0);
        return Arrays.copyOf(checksum, AES128LENGTH);
    }

    private static byte[] replaceLastKeyByte(final byte[] key, final Mode mode) {
        final byte[] keyBytes = new byte[key.length + OFFSETLENGTH];
        System.arraycopy(key, 0, keyBytes, 0, key.length);
        switch (mode) {
            case ENC:
                keyBytes[keyBytes.length - 1] = ENCLASTBYTE;
                break;
            case MAC:
                keyBytes[keyBytes.length - 1] = MACLASTBYTE;
                break;
            case PASSWORD:
                keyBytes[keyBytes.length - 1] = PASSWORDLASTBYTE;
                break;
            default:
                keyBytes[keyBytes.length - 1] = ENCLASTBYTE;
        }
        return keyBytes;
    }
}
