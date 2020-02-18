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
 * Exception if the used array for object system version has not the correct length of 3 octets
 */
public class WrongObjectSystemVersionArraySizeException extends RuntimeException {

    private static final String MESSAGE = "Illegal Byte-Array Size for ObjectSystemVersion. The object system version should have a length of 3 octets.";
    private static final long serialVersionUID = -4343715190993353727L;

    /**
     * constructor
     */
    public WrongObjectSystemVersionArraySizeException() {
        super(MESSAGE);
    }
}
