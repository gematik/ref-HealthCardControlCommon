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

package de.gematik.ti.healthcard.control.common.integration.card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.healthcardaccess.AbstractHealthCardCommand;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.cardobjects.ApplicationIdentifier;
import de.gematik.ti.healthcardaccess.cardobjects.FileIdentifier;
import de.gematik.ti.healthcardaccess.commands.ReadCommand;
import de.gematik.ti.healthcardaccess.commands.SelectCommand;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.healthcardaccess.operation.Subscriber;
import de.gematik.ti.healthcardaccess.result.Response;
import de.gematik.ti.healthcardaccess.sanitychecker.BER_TLV;
import de.gematik.ti.utils.codec.Hex;
import de.gematik.ti.utils.primitives.Bytes;

/**
 * Read content of any Ef safe in regard to different MaxResponseLength of various CardReader <br/>
 * Required parameters are:
 * <li>healthCard</li>
 * <li>numberOfOctet as bytesToRead</li>
 * <li>maxResponseLength of cardReader</li>
 */
public class ReadCommandSafeExecutor {
    private final Subscriber<byte[]> subscriber;
    private final Subscriber<Response> subscriberSelection;
    private static final Logger LOG = LoggerFactory.getLogger(ReadCommandSafeExecutor.class);
    private final IHealthCard cardToRead;
    private final int maxResponseLength;
    private int[] bytesToRead = new int[] { 0 };
    private int offset = 0;
    private final byte[][] readData = { new byte[] {} };

    public ReadCommandSafeExecutor(final IHealthCard cardToRead, final int maxResponseLength) {
        this.cardToRead = cardToRead;
        this.maxResponseLength = maxResponseLength;
        subscriber = getSubscriber();
        subscriberSelection = getSubscriberSelection();
    }

    /**
     * For reading an EF under a DF
     * @param dfAid
     * @param efFid
     * @return
     */
    public ResultOperation<byte[]> readSafe(final ApplicationIdentifier dfAid, final FileIdentifier efFid) {
        final AbstractHealthCardCommand selectCommandDf = new SelectCommand(dfAid);
        final AbstractHealthCardCommand selectCommandEf = new SelectCommand(efFid, false, true, 65535);
        selectCommandDf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .flatMap(__ -> selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)).subscribe(
                subscriberSelection);
        LOG.debug("maxResponseLength:" + maxResponseLength);
        LOG.debug("bytesToRead[0]:" + bytesToRead[0]);
        while (bytesToRead[0] > 0) {
            final int readBytesLength;
            // get ReadCommand
            if (bytesToRead[0] > maxResponseLength) {
                readBytesLength = maxResponseLength;
            } else {
                readBytesLength = bytesToRead[0];
            }
            final AbstractHealthCardCommand readCommand = new ReadCommand(offset, readBytesLength);
            LOG.debug("readBytesLength: " + readBytesLength);
            // run command
            selectCommandDf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)
                    .flatMap(__ -> selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult))
                    .flatMap(__ -> readCommand.executeOn(cardToRead).map(Response::getResponseData).map(bytes -> {
                        LOG.debug("readBytesStep: " + Hex.encodeHexString(bytes));
                        readData[0] = Bytes.concatNullables(readData[0], bytes);
                        return bytes;
                    })).subscribe(subscriber);

            LOG.debug("readBytesCollect: " + Hex.encodeHexString(readData[0]));
            offset += readBytesLength;
            bytesToRead[0] -= readBytesLength;
        }
        return ResultOperation.unitRo(readData[0]);
    }

    private int parsePositionLogOfFile(final byte[] responseData) {
        final String valueResp = Hex.encodeHexString(responseData);
        LOG.debug("valueResp: " + valueResp);
        final String value = new BER_TLV(valueResp).findTag("C5").getValue();
        final int positionLogOfFile = 2 * Integer.parseInt(value, 16);
        LOG.debug("positionLogOfFile.length: " + positionLogOfFile);
        return positionLogOfFile;
    }

    /**
     * For reading an EF under a SubDF like DF.NFD under DF.HCA
     * @param dfAid
     * @param efFid
     * @return
     */
    public ResultOperation<byte[]> readSafe(final ApplicationIdentifier dfAid, final ApplicationIdentifier subDfAid, final FileIdentifier efFid) {
        final AbstractHealthCardCommand selectCommandDf = new SelectCommand(dfAid);
        final AbstractHealthCardCommand selectCommandSubDf = new SelectCommand(subDfAid);
        final AbstractHealthCardCommand selectCommandEf = new SelectCommand(efFid, false, true, 65535);
        selectCommandDf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .flatMap(__ -> selectCommandSubDf.executeOn(cardToRead)).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .flatMap(__ -> selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)).subscribe(
                subscriberSelection);

        LOG.debug("maxResponseLength:" + maxResponseLength);
        LOG.debug("bytesToRead[0]:" + bytesToRead[0]);
        while (bytesToRead[0] > 0) {
            final int readBytesLength;
            // get ReadCommand
            if (bytesToRead[0] > maxResponseLength) {
                readBytesLength = maxResponseLength;
            } else {
                readBytesLength = bytesToRead[0];
            }
            final AbstractHealthCardCommand readCommand = new ReadCommand(offset, readBytesLength);
            LOG.debug("readBytesLength: " + readBytesLength);
            // run command
            selectCommandDf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)
                    .flatMap(__ -> selectCommandSubDf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult))
                    .flatMap(__ -> selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult))
                    .flatMap(__ -> readCommand.executeOn(cardToRead).map(Response::getResponseData).map(bytes -> {
                        LOG.debug("readBytesStep: " + Hex.encodeHexString(bytes));
                        readData[0] = Bytes.concatNullables(readData[0], bytes);
                        return bytes;
                    })).subscribe(subscriber);

            LOG.debug("readBytesCollect: " + Hex.encodeHexString(readData[0]));
            offset += readBytesLength;
            bytesToRead[0] -= readBytesLength;
        }
        return ResultOperation.unitRo(readData[0]);
    }

    /**
     * For reading an EF under MF
     * @param efFid
     * @return
     */
    public ResultOperation<byte[]> readSafe(final FileIdentifier efFid) {
        final AbstractHealthCardCommand selectCommandEf = new SelectCommand(efFid, false, true, 65535);
        selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult).subscribe(subscriberSelection);

        LOG.debug("maxResponseLength:" + maxResponseLength);
        LOG.debug("bytesToRead[0]:" + bytesToRead[0]);
        while (bytesToRead[0] > 0) {
            final int readBytesLength;
            // get ReadCommand
            if (bytesToRead[0] > maxResponseLength) {
                readBytesLength = maxResponseLength;
            } else {
                readBytesLength = bytesToRead[0];
            }
            final AbstractHealthCardCommand readCommand = new ReadCommand(offset, readBytesLength);
            LOG.debug("readBytesLength: " + readBytesLength);
            // run command
            selectCommandEf.executeOn(cardToRead).validate(Response.ResponseStatus.SUCCESS::validateResult)
                    .flatMap(__ -> readCommand.executeOn(cardToRead).map(Response::getResponseData).map(bytes -> {
                        LOG.debug("readBytesStep: " + Hex.encodeHexString(bytes));
                        readData[0] = Bytes.concatNullables(readData[0], bytes);
                        return bytes;
                    })).subscribe(subscriber);

            LOG.debug("readBytesCollect: " + Hex.encodeHexString(readData[0]));
            offset += readBytesLength;
            bytesToRead[0] -= readBytesLength;
        }
        return ResultOperation.unitRo(readData[0]);
    }

    private Subscriber<byte[]> getSubscriber() {
        return new Subscriber<byte[]>() {
            @Override
            public void onSuccess(final byte[] value) {
                LOG.debug("Subscriber - get value: " + Hex.encodeHexString(value));
            }

            @Override
            public void onError(final Throwable t) throws RuntimeException {
                LOG.error("Subscriber - get error: " + t.getMessage());
            }
        };
    }

    private Subscriber<Response> getSubscriberSelection() {
        return new Subscriber<Response>() {
            @Override
            public void onSuccess(final Response response) {
                de.gematik.ti.healthcard.control.common.integration.card.ReadCommandSafeExecutor.this.bytesToRead = new int[] { 0 };
                de.gematik.ti.healthcard.control.common.integration.card.ReadCommandSafeExecutor.this.bytesToRead[0] = parsePositionLogOfFile(response.getResponseData());
            }

            @Override
            public void onError(final Throwable t) throws RuntimeException {
                LOG.error("Subscriber - get error: " + t.getMessage());
            }
        };
    }
}
