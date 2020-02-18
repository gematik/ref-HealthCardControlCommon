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

import de.gematik.ti.healthcardaccess.entities.Version2;
import de.gematik.ti.utils.codec.Hex;

/**
 * Extract the Card Generation from objectSystemVersion and return the representing enum value
 */
public class CardGenerationExtractor {

    private static final int RADIX_16 = 16;
    private static final int MIN_VERSION_LENGTH = 3;

    private CardGenerationExtractor() {
        // Nothing
    }

    /**
     * Extract the Card Generation from objectSystemVersion byte array and return the representing enum value
     *
     * @param objectSystemVersion byte array with 3 octets object system version
     * @return CardGeneration enum
     */
    public static CardGeneration getCardGeneration(final byte[] objectSystemVersion) {
        check(objectSystemVersion);
        final int major = convertToInt(objectSystemVersion[0]);
        final int minor = convertToInt(objectSystemVersion[1]);
        final int release = convertToInt(objectSystemVersion[2]);

        final int version = Integer.parseInt(String.format("%02d%02d%02d", major, minor, release));
        return CardGeneration.getCardGeneration(version);
    }

    /**
     * Extract the Card Generation from Version2 and return the representing enum value
     * @param version2 Version2 Object
     * @return CardGeneration enum
     */
    public static CardGeneration getCardGeneration(final Version2 version2) {
        return getCardGeneration(version2.getObjectSystemVersion());
    }

    private static Integer convertToInt(final byte octet) {
        return Integer.valueOf(Hex.encodeHexString(new byte[] { octet }), RADIX_16);
    }

    private static void check(final byte[] objectSystemVersion) {
        if (objectSystemVersion.length != MIN_VERSION_LENGTH) {
            throw new WrongObjectSystemVersionArraySizeException();
        }
    }
}
