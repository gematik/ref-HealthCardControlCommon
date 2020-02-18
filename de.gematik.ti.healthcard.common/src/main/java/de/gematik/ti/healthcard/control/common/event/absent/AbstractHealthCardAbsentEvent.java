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

package de.gematik.ti.healthcard.control.common.event.absent;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.healthcard.control.common.event.AbstractHealthCardEvent;
import de.gematik.ti.healthcardaccess.IHealthCard;

/**
 * Represent a health card absent event
 */
public abstract class AbstractHealthCardAbsentEvent<T extends IHealthCard> extends AbstractHealthCardEvent<T> {

    /**
     * Create a new instance of Health card absent event
     * @param cardReader - card reader object with physical card
     * @param healthCard - health card object
     */
    protected AbstractHealthCardAbsentEvent(final ICardReader cardReader, final T healthCard) {
        super(cardReader, healthCard);
    }
}