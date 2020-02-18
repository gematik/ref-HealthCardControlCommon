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

package de.gematik.ti.healthcard.control.common.verifyPin;

import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.cardreader.provider.api.events.card.CardAbsentEvent;
import de.gematik.ti.healthcard.control.common.CardFunction;
import de.gematik.ti.healthcard.control.common.HealthcardCommonRuntimeException;
import de.gematik.ti.healthcardaccess.IHealthCard;
import de.gematik.ti.healthcardaccess.cardobjects.ApplicationIdentifier;
import de.gematik.ti.healthcardaccess.cardobjects.Format2Pin;
import de.gematik.ti.healthcardaccess.cardobjects.Password;
import de.gematik.ti.healthcardaccess.commands.GetPinStatusCommand;
import de.gematik.ti.healthcardaccess.commands.SelectCommand;
import de.gematik.ti.healthcardaccess.commands.VerifyCommand;
import de.gematik.ti.healthcardaccess.operation.CheckedSupplier;
import de.gematik.ti.healthcardaccess.operation.Result;
import de.gematik.ti.healthcardaccess.operation.ResultOperation;
import de.gematik.ti.healthcardaccess.result.Response;
import de.gematik.ti.healthcardaccess.result.Response.ResponseStatus;
import de.gematik.ti.openhealthcard.events.control.CommonEventTransmitter;

/**
 * execute pin verification
 */
public class PinVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(PinVerifier.class);

    private final IHealthCard cardHc;

    private VerifyState verifyState = VerifyState.verifyRequired;

    /**
     * init the card
     *
     * @param card
     */
    public PinVerifier(final IHealthCard card) {
        cardHc = card;
    }

    public static int[] stringToIntarray(final String value) {
        final int[] intArray = new int[value.length()];
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                LOG.error("Contains an invalid digit");
                break;
            }
            intArray[i] = Integer.parseInt(String.valueOf(value.charAt(i)));
        }
        return intArray;
    }

    /**
     * do verify on CardType (eGK2 or eGK2.1)
     *
     * @param pinValue
     * @param pinConfiguration
     * @return
     */
    public ResultOperation<PinResult> verifyPin(final int[] pinValue, final PinConfiguration pinConfiguration) {
        if (CardFunction.isCardValid(cardHc)) {
            final String[] appids = pinConfiguration.getAppIds();
            final int pwid = pinConfiguration.getPwid();
            if (verifyState == VerifyState.verifyRequired) {
                return verifyPin(pinValue, appids, pwid);
            }
        }
        throw new HealthcardCommonRuntimeException("Card not valid for verifyPin: " + cardHc.getStatus().getClass().getSimpleName());
    }

    private ResultOperation<Response> getSelectRootResult() {
        return new SelectCommand(false, false).executeOn(cardHc).validate(ResponseStatus.SUCCESS::validateResult);
    }

    @Subscribe
    private void hookAbsentEvent(final CardAbsentEvent cardAbsentEvent) {
        verifyState = VerifyState.verifyRequired;
    }

    /**
     * solution for <br>eGK2.1</b>
     *
     * @param pinValue
     * @param pwid
     * @return
     */
    private ResultOperation<PinResult> verifyPin(final int[] pinValue, final String[] appids, final int pwid) {
        LOG.debug("appids: {}, pwid: {}", appids, pwid);
        final Password password = new Password(pwid);
        final ResultOperation<Response> selectRootResult = getSelectRootResult();
        final boolean dirSpecific = appids.length > 0;
        final ResultOperation<PinResult> pinResultResultOperation = flapMapSelections(appids, selectRootResult, 0)
                .flatMap(__ -> new GetPinStatusCommand(password, dirSpecific).executeOn(cardHc))
                .map(Response::getResponseStatus).map(ResponseStatus::name).map(PinResult::new)
                .validate(pinResult -> {
                    return validatePinResult(pinResult);
                })
                .flatMap(pinResult -> new VerifyCommand(password, dirSpecific, new Format2Pin(pinValue)).executeOn(cardHc)
                        .map(Response::getResponseStatus).map(status -> pinResult.setPinVerifiSuccess(status == ResponseStatus.SUCCESS)));

        verifyState = VerifyState.verifyValid;
        return pinResultResultOperation;
    }

    private Result<PinResult> validatePinResult(final PinResult pr) {
        if (pr.isPinVerifiSuccess()) {
            return Result.success(pr);
        } else {
            final HealthcardCommonRuntimeException healthcardCommonRuntimeException;
            if (pr.getVerifyResultText().equals("PASSWORD_DISABLED")) {
                healthcardCommonRuntimeException = new HealthcardCommonRuntimeException(PinVerifyError.blocking.toString());
            } else {
                healthcardCommonRuntimeException = new HealthcardCommonRuntimeException(PinVerifyError.accessErr.toString());
            }
            CommonEventTransmitter.postError(healthcardCommonRuntimeException);
            LOG.error(pr.getVerifyResultText());
            final CheckedSupplier<?> check = new CheckedSupplier<Object>() {
                @Override
                public PinResult get() throws Throwable {
                    return pr;
                }
            };
            final Result<PinResult> failure = (Result<PinResult>) Result.evaluate(() -> check.get());
            return failure;
        }
    }

    /**
     * next begins from '0', continue on with 'ro', whole eitries in aids will be selected from end to begin. ie. a, b, c, select c-> b -> a
     * @param appids
     * @param ro
     * @param next
     * @return
     */
    private ResultOperation<Response> flapMapSelections(final String[] appids, final ResultOperation<Response> ro, int next) {
        if (next > appids.length - 1) {
            return ro;
        }
        final String aid = appids[next];
        next++;
        return flapMapSelections(appids, ro, next)
                .flatMap(__ -> new SelectCommand(new ApplicationIdentifier(aid)).executeOn(cardHc))
                .validate(Response.ResponseStatus.SUCCESS::validateResult);

    }

    /**
     * represent if a valid Verification already done and still exists or not <br/>
     * if a card is verified and no reset then it's state stay as verifiedValid. <br/>
     * if card is reset, then the state is changed as verifyRequired
     */
    enum VerifyState {
        verifyValid,
        verifyRequired
    }
}
