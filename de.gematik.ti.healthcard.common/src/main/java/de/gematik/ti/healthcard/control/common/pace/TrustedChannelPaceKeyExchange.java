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

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.asn1.*;
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import cardfilesystem.egk21mf.Ef;
import de.gematik.ti.healthcard.control.common.Utilities;
import de.gematik.ti.healthcard.control.common.exceptions.VerifyReceivedMacPiccException;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.cardobjects.FileIdentifier;
import de.gematik.ti.healthcardaccess.cardobjects.Key;
import de.gematik.ti.healthcardaccess.commands.GeneralAuthenticateCommand;
import de.gematik.ti.healthcardaccess.commands.ManageSecurityEnvironmentCommand;
import de.gematik.ti.healthcardaccess.commands.ReadCommand;
import de.gematik.ti.healthcardaccess.commands.SelectCommand;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.healthcardaccess.result.Response;
import de.gematik.ti.openhealthcard.events.response.entities.PaceKey;
import de.gematik.ti.utils.codec.Hex;
import de.gematik.ti.utils.primitives.Bytes;

/**
 * Opens a secure PACE Channel for secure messaging
 */
public class TrustedChannelPaceKeyExchange { // NOCS(SAB): Zum Aufbau des Trusted Channel notwendig

    private static final Logger LOG = LoggerFactory.getLogger(TrustedChannelPaceKeyExchange.class);
    private static final String TAG = "TrustedChannelPaceKeyExchange: ";

    private static final int SECRET_KEY_REFERENCE = 2; // Reference of secret key for PACE (CAN)"
    private static final int AES_BLOCK_SIZE = 16;
    private static final int BYTE_LENGTH = 8;
    private static final int MAX = 64;
    private static final int TAG_6 = 6;
    private static final int TAG_49 = 0x49;

    private final IHealthCard card;
    private final String can;
    private final PaceInfo[] paceInfo = new PaceInfo[1];

    private Mac mac;
    private ECCurve.Fp curve;
    private ECPoint ecPointG;
    private BigInteger nonceSInt;
    private BigInteger pcdSkX1;
    private BigInteger pcdSkX2;
    private byte[] kEnc;
    private byte[] kMac;
    private byte[] authTokenX;

    private SecureRandom randomGenerator;

    /**
     * Constructor
     * @param card
     *      IHealthCardObject
     * @param can
     *      CardAccessNumber
     */
    public TrustedChannelPaceKeyExchange(final IHealthCard card, final String can) {
        this.can = can;
        this.card = card;
    }

    /**
     * Negotiate the PaceKey and return the object
     * @return PaceKey
     */
    public ResultOperation<PaceKey> negotiatePaceKey() {

        return new SelectCommand(false, true).executeOn(card).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .flatMap(__ -> new SelectCommand(new FileIdentifier(Ef.CardAccess.FID), false).executeOn(card))
                .validate(Response.ResponseStatus.SUCCESS::validateResult)
                .flatMap(__ -> new ReadCommand().executeOn(card).validate(Response.ResponseStatus.SUCCESS::validateResult).map(Response::getResponseData)
                        .map(cardAccessBytes -> paceInfo[0] = new PaceInfo(cardAccessBytes)))
                .flatMap(__ -> new ManageSecurityEnvironmentCommand(
                        ManageSecurityEnvironmentCommand.MseUseCase.KEY_SELECTION_FOR_SYMMETRIC_CARD_CONNECTION_WITHOUT_CURVES, new Key(SECRET_KEY_REFERENCE),
                        false, paceInfo[0].getPaceInfoProtocolBytes()).executeOn(card).validate(Response.ResponseStatus.SUCCESS::validateResult))
                .flatMap(__ -> new GeneralAuthenticateCommand(true).executeOn(card)).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .map(Response::getResponseData).flatMap(this::generateEphemeralPublicKeyFirstECDH).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .map(Response::getResponseData).flatMap(pk1Pcd -> new GeneralAuthenticateCommand(true, pk1Pcd, 1).executeOn(card))
                .validate(Response.ResponseStatus.SUCCESS::validateResult).map(Response::getResponseData).flatMap(this::generateEphemeralPublicKeySecondECDH)
                .validate(Response.ResponseStatus.SUCCESS::validateResult).map(Response::getResponseData)
                .flatMap(pk2Pcd -> new GeneralAuthenticateCommand(true, pk2Pcd, 3).executeOn(card)).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .map(Response::getResponseData).flatMap(this::createMacPcdForMutualAuthentication).validate(Response.ResponseStatus.SUCCESS::validateResult)
                .map(Response::getResponseData).flatMap(macPcd -> new GeneralAuthenticateCommand(false, macPcd, 5).executeOn(card))
                .validate(Response.ResponseStatus.SUCCESS::validateResult).map(Response::getResponseData).flatMap(this::verifyReceivedMacPicc)
                .flatMap(receivedMacPiccIsVerified -> {
                    if (receivedMacPiccIsVerified) {
                        return createPaceKey();
                    } else {
                        throw new VerifyReceivedMacPiccException();
                    }
                });
    }

    private ResultOperation<Response> generateEphemeralPublicKeyFirstECDH(final byte[] nonceZBytes) {
        final String parameter = paceInfo[0].getParameterIDString();
        final byte[] nonceZBytesEncoded;

        try {
            nonceZBytesEncoded = Utilities.getKeyObjectEncoded(nonceZBytes);
            final byte[] canBytes = can.getBytes();
            final byte[] nonceS = new byte[AES_BLOCK_SIZE];
            final byte[] aes128Key = KeyDerivationFunction.getAES128Key(canBytes, KeyDerivationFunction.Mode.PASSWORD);
            final KeyParameter encKey = new KeyParameter(aes128Key);
            final BlockCipher cipher = new AESEngine();

            final ECNamedCurveParameterSpec ecNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(parameter);
            curve = (ECCurve.Fp) ecNamedCurveParameterSpec.getCurve();
            ecPointG = ecNamedCurveParameterSpec.getG();
            randomGenerator = new SecureRandom();
            final Random rnd = new Random();
            randomGenerator.setSeed(rnd.nextLong());
            cipher.init(false, encKey);
            cipher.processBlock(nonceZBytesEncoded, 0, nonceS, 0);
            nonceSInt = new BigInteger(1, nonceS);
            byte[] pk1Pcd = new byte[curve.getFieldSize() / BYTE_LENGTH];
            randomGenerator.nextBytes(pk1Pcd);
            pcdSkX1 = new BigInteger(1, pk1Pcd);
            final ECPoint pcdPkS1 = ecPointG.multiply(pcdSkX1);
            pk1Pcd = pcdPkS1.getEncoded(false);
            return ResultOperation.unitRo(new Response(Response.ResponseStatus.SUCCESS, pk1Pcd));
        } catch (final IOException e) {
            LOG.error(TAG, "Failed to get encoded NonceZ " + e.getMessage());
            return ResultOperation.unitRo(new Response(Response.ResponseStatus.UNKNOWN_EXCEPTION, null));
        }
    }

    private ResultOperation<Response> generateEphemeralPublicKeySecondECDH(final byte[] pk1PiccBytes) {
        final byte[] pk1PiccBytesEncoded;
        try {
            pk1PiccBytesEncoded = Utilities.getKeyObjectEncoded(pk1PiccBytes);
            final ECPoint.Fp y1 = (ECPoint.Fp) Utilities.byteArrayToECPoint(pk1PiccBytesEncoded, curve);
            final ECPoint.Fp sharedSecretP = (ECPoint.Fp) y1.multiply(pcdSkX1);
            final ECPoint pointGS = ecPointG.multiply(nonceSInt).add(sharedSecretP);
            final byte[] x2 = new byte[curve.getFieldSize() / BYTE_LENGTH];
            randomGenerator.nextBytes(x2);
            pcdSkX2 = new BigInteger(1, x2);
            final ECPoint pcdPkS2 = pointGS.multiply(pcdSkX2);
            final byte[] pk2Pcd = pcdPkS2.getEncoded(false);
            final ECPoint.Fp ecPointX2 = (ECPoint.Fp) Utilities.byteArrayToECPoint(pk2Pcd, curve);
            authTokenX = createAsn1AuthToken(ecPointX2, paceInfo[0].getProtocolID());

            return ResultOperation.unitRo(new Response(Response.ResponseStatus.SUCCESS, pk2Pcd));
        } catch (final IOException e) {
            LOG.error(TAG, "Failed to get encoded pk1PiccBytes " + e.getMessage());
            return ResultOperation.unitRo(new Response(Response.ResponseStatus.KEY_INVALID, null));
        }
    }

    private ResultOperation<Response> createMacPcdForMutualAuthentication(final byte[] pk2Picc) {
        final byte[] pk2PiccEncoded;
        try {
            final String protocolID = paceInfo[0].getProtocolID();
            pk2PiccEncoded = Utilities.getKeyObjectEncoded(pk2Picc);

            final ECPoint piccPkY2 = Utilities.byteArrayToECPoint(pk2PiccEncoded, curve);
            final ECPoint.Fp sharedSecretK = (ECPoint.Fp) piccPkY2.multiply(pcdSkX2);
            final byte[] sharedSecretKBytes = Bytes.bigIntToByteArray(sharedSecretK.normalize().getXCoord().toBigInteger());

            kEnc = KeyDerivationFunction.getAES128Key(sharedSecretKBytes, KeyDerivationFunction.Mode.ENC);
            kMac = KeyDerivationFunction.getAES128Key(sharedSecretKBytes, KeyDerivationFunction.Mode.MAC);

            final BlockCipher cipher = new AESEngine();
            final KeyParameter keyP = new KeyParameter(kMac);
            mac = new CMac(cipher, MAX);
            mac.init(keyP);

            final ECPoint.Fp y2 = (ECPoint.Fp) Utilities.byteArrayToECPoint(pk2PiccEncoded, curve);
            final byte[] authTokenY = createAsn1AuthToken(y2, protocolID);
            final byte[] macPcd = generateMacPcdPicc2(authTokenY);

            return ResultOperation.unitRo(new Response(Response.ResponseStatus.SUCCESS, macPcd));
        } catch (final IOException e) {
            throw new RuntimeException("Error on creating MacPcd for Mutual Authentication " + e.getMessage());
        }
    }

    private ResultOperation<Boolean> verifyReceivedMacPicc(final byte[] macPiccBytes) {

        final byte[] macPicc2 = generateMacPcdPicc2(authTokenX);
        final byte[] macPicc;
        try {
            macPicc = Utilities.getKeyObjectEncoded(macPiccBytes);
            if (Hex.encodeHexString(macPicc).equals(Hex.encodeHexString(macPicc2))) {
                return ResultOperation.unitRo(true);
            } else {
                throw new RuntimeException("calculated macPicc does not match received macPicc");
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error on encoding key object " + e.getMessage());
        }
    }

    private byte[] createAsn1AuthToken(final ECPoint.Fp point, final String protocolID) {
        final ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(new ASN1ObjectIdentifier(protocolID));
        asn1EncodableVector.add(new DERTaggedObject(false, TAG_6, new DEROctetString(point.getEncoded(false))));
        byte[] authToken = new byte[0];
        try {
            authToken = new DERApplicationSpecific(TAG_49, asn1EncodableVector).getEncoded();
        } catch (final IOException e) {
            LOG.error(TAG, "Failed to encoding ASN1 AuthToken" + e.getMessage());
        }
        return authToken;
    }

    private byte[] generateMacPcdPicc2(final byte[] authToken) {

        mac.update(authToken, 0, authToken.length);
        final byte[] key = new byte[mac.getMacSize()];
        mac.doFinal(key, 0);
        return key;
    }

    private ResultOperation<PaceKey> createPaceKey() {
        final PaceKey paceKey = new PaceKey(kEnc, kMac);
        return ResultOperation.unitRo(paceKey);
    }
}
