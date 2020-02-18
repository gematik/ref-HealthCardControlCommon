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

package de.gematik.ti.healthcard.control.common.event;

import de.gematik.ti.cardreader.provider.api.ICardReader;
import de.gematik.ti.cardreader.provider.api.events.AbstractCardReaderEvent;
import de.gematik.ti.healthcardaccess.IHealthCard;

/**
 * Represent a health card event
 */
public abstract class AbstractHealthCardEvent<T extends IHealthCard> extends AbstractCardReaderEvent {
    private final T healthCard;

    protected AbstractHealthCardEvent(final ICardReader cardReader, final T healthCard) {
        super(cardReader);
        this.healthCard = healthCard;
    }

    /**
     * The health card for this event
     * @return instance of IHealthCard
     */
    public T getHealthCard() {
        return healthCard;
    }
}
