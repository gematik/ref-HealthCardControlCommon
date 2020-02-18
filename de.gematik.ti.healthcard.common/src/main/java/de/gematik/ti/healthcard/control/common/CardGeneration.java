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

/**
 * Represent the card generation of health card
 *

 * |===
 * | Version   | Version with 2 digits | INT Value for Version | Card generation
 * | < 3.0.3   |  03.00.03             | 30003                 | G1
 * | < 4.0.0   |  04.00.00             | 40000                 | G1P
 * | >= 4.0.0  |  04.00.00             | 40000                 | G2
 * | >= 4.4.0  |  04.04.00             | 40400                 | G2_1
 * |===
 */
public enum CardGeneration {
    G1,
    G1P,
    G2,
    G2_1,
    UNKNOWN;

    private static final int VERSION_3_0_3 = 30003;
    private static final int VERSION_4_0_0 = 40000;
    private static final int VERSION_4_4_0 = 40400;

    /**
     * Return the ENUM for ObjectSystemVersion
     *
     * @param version value like 30003, 40000 (for details see class description)
     * @return Generation Value
     */
    public static CardGeneration getCardGeneration(final int version) {
        if (0 < version && version < VERSION_3_0_3) { // V < 3.0.3
            return G1;
        } else if (0 < version && version < VERSION_4_0_0) { // 3.0.3 <= V < 4.0.0
            return G1P;
        } else if (0 < version && version < VERSION_4_4_0) { // 4.0.0 <= V < 4.4.0
            return G2;
        } else if (0 < version) { // V >= 4.4.0
            return G2_1;
        } else {
            return UNKNOWN;
        }
    }
}
