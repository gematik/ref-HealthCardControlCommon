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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.healthcard.control.common.CardFunction;
import de.gematik.ti.healthcard.control.common.HealthcardCommonRuntimeException;
import de.gematik.ti.healthcardaccess.IHealthCard;

/**
 * Configuration of password objects of gematik ehealth card
 */
public final class PinContainer {
    private static final Logger LOG = LoggerFactory.getLogger(PinContainer.class);
    private static final String[] EMPTY_APPIDS = new String[] {};
    /**
     * all pw-objects in eGK2 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}} <br/>
     * Order of appIds:  for instance {x2, x1} of which x1 will be seleted at first, then x2.
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_EGK2 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -4236141933400824673L;

        {
            final String aidHCA = cardfilesystem.egk2mf.Df.Hca.AID;
            put(cardfilesystem.egk2mf.Pin.Ch.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.egk2mf.Pin.Ch.PWID));
            // Pin.Qes
            put(cardfilesystem.egk2mf.df.qes.Pin.Qes.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.Df.Qes.AID }, cardfilesystem.egk2mf.df.qes.Pin.Qes.PWID));
            // MrPin.Home
            put(cardfilesystem.egk2mf.MrPin.Home.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.egk2mf.MrPin.Home.PWID));
            // MrPin.Nfd
            put(cardfilesystem.egk2mf.df.hca.df.nfd.MrPin.Nfd.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.NFD.AID, aidHCA }, cardfilesystem.egk2mf.df.hca.df.nfd.MrPin.Nfd.PWID));
            // MrPin.NfdRead
            put(cardfilesystem.egk2mf.df.hca.df.nfd.MrPin.NfdRead.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.NFD.AID, aidHCA },
                            cardfilesystem.egk2mf.df.hca.df.nfd.MrPin.NfdRead.PWID));
            // MrPin.Dpe
            put(cardfilesystem.egk2mf.df.hca.df.dpe.MrPin.Dpe.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.DPE.AID, aidHCA }, cardfilesystem.egk2mf.df.hca.df.dpe.MrPin.Dpe.PWID));
            // MrPin.DpeRead
            put(cardfilesystem.egk2mf.df.hca.df.dpe.MrPin.DpeRead.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.DPE.AID, aidHCA },
                            cardfilesystem.egk2mf.df.hca.df.dpe.MrPin.DpeRead.PWID));
            // MrPin.Gdd
            put(cardfilesystem.egk2mf.df.hca.df.gdd.MrPin.Gdd.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.GDD.AID, aidHCA }, cardfilesystem.egk2mf.df.hca.df.gdd.MrPin.Gdd.PWID));
            // MrPin.Amts
            put(cardfilesystem.egk2mf.df.hca.df.amts.MrPin.Amts.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.Amts.AID, aidHCA },
                            cardfilesystem.egk2mf.df.hca.df.amts.MrPin.Amts.PWID));
            // MrPin.Ose
            put(cardfilesystem.egk2mf.df.hca.df.ose.MrPin.Ose.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk2mf.df.hca.Df.OSE.AID, aidHCA }, cardfilesystem.egk2mf.df.hca.df.ose.MrPin.Ose.PW));

        }
    };

    /**
     * all pw-objects in eGK2.1 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}}
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_EGK21 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -6641841460481058190L;

        {
            put(cardfilesystem.egk21mf.Pin.Ch.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.egk21mf.Pin.Ch.PWID));

            put(cardfilesystem.egk21mf.df.qes.Pin.Qes.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.egk21mf.Df.Qes.AID }, cardfilesystem.egk21mf.df.qes.Pin.Qes.PWID));

            put(cardfilesystem.egk21mf.MrPin.Home.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Home.PWID));

            put(cardfilesystem.egk21mf.MrPin.Nfd.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Nfd.PWID));

            put(cardfilesystem.egk21mf.MrPin.Dpe.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Dpe.PWID));

            put(cardfilesystem.egk21mf.MrPin.Gdd.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Gdd.PWID));
            put(cardfilesystem.egk21mf.MrPin.Amts.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Amts.PWID));

            put(cardfilesystem.egk21mf.MrPin.Ose.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.Ose.PWID));

            put(cardfilesystem.egk21mf.MrPin.NfdRead.class.getCanonicalName(), new PinConfiguration(
                    EMPTY_APPIDS, cardfilesystem.egk21mf.MrPin.NFD_READ.PWID));

        }
    };
    /**
     * all pw-objects in hba2 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}}
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_HBA2 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -4029077305228117960L;

        {
            put(cardfilesystem.hba2mf.df.auto.Pin.Auto.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba2mf.Df.AUTO.AID }, cardfilesystem.hba2mf.df.auto.Pin.Auto.PWID));

            put(cardfilesystem.hba2mf.df.auto.Pin.So.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba2mf.Df.AUTO.AID }, cardfilesystem.hba2mf.df.auto.Pin.So.PWID));

            put(cardfilesystem.hba2mf.df.qes.Pin.Qes.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba2mf.Df.Qes.AID }, cardfilesystem.hba2mf.df.qes.Pin.Qes.PWID));

            put(cardfilesystem.hba2mf.Pin.Ch.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.hba2mf.Pin.Ch.PWID));
        }
    };
    /**
     * all pw-objects in hba2.1 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}}
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_HBA21 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -135520443669011378L;

        {
            put(cardfilesystem.hba21mf.df.auto.Pin.Auto.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba21mf.Df.AUTO.AID }, cardfilesystem.hba21mf.df.auto.Pin.Auto.PWID));

            put(cardfilesystem.hba21mf.df.auto.Pin.So.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba21mf.Df.AUTO.AID }, cardfilesystem.hba21mf.df.auto.Pin.So.PWID));

            put(cardfilesystem.hba21mf.df.qes.Pin.Qes.class.getCanonicalName(),
                    new PinConfiguration(new String[] { cardfilesystem.hba21mf.Df.Qes.AID }, cardfilesystem.hba21mf.df.qes.Pin.Qes.PWID));

            put(cardfilesystem.hba21mf.Pin.Ch.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.hba21mf.Pin.Ch.PWID));
        }
    };
    /**
     * all pw-objects in smcb2 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}}
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_SMCB2 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -227817909401546970L;

        {
            put(cardfilesystem.smcb2mf.Pin.Smc.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.smcb2mf.Pin.Smc.PWID));
        }
    };
    /**
     * all pw-objects in smcb2.1 <br/>
     * pattern: {pw-class-name, pinConfig { {all-application-ids},  pwid}}
     */
    private static final Map<String, PinConfiguration> PINCONTAINER_SMCB21 = new HashMap<String, PinConfiguration>() {
        private static final long serialVersionUID = -4937759622994400575L;

        {
            put(cardfilesystem.smcb21mf.Pin.Smc.class.getCanonicalName(), new PinConfiguration(EMPTY_APPIDS, cardfilesystem.smcb21mf.Pin.Smc.PWID));
        }
    };

    /**
     * find PinConfiguration of the card
     * @param card
     * @return
     */
    private static Map<String, PinConfiguration> getContainer(final IHealthCard card) {
        LOG.debug("getContainer: " + CardFunction.getCardType(card));
        switch (CardFunction.getCardType(card).toString()) {
            case "de.gematik.ti.healthcardaccess.healthcards.Egk2":
                return PINCONTAINER_EGK2;
            case "de.gematik.ti.healthcardaccess.healthcards.Egk21":
                return PINCONTAINER_EGK21;
            case "de.gematik.ti.healthcardaccess.healthcards.Hba2":
                return PINCONTAINER_HBA2;
            case "de.gematik.ti.healthcardaccess.healthcards.Hba21":
                return PINCONTAINER_HBA21;
            case "de.gematik.ti.healthcardaccess.healthcards.Smcb2":
                return PINCONTAINER_SMCB2;
            case "de.gematik.ti.healthcardaccess.healthcards.Smcb21":
                return PINCONTAINER_SMCB21;
        }
        throw new HealthcardCommonRuntimeException("Not supported card " + card.getClass().getName());
    }

    /**
     * find the PinConfigration of card for certain pinType
     * @param card
     * @param pinType
     * @return
     */
    public static PinConfiguration getPinConfiguration(final IHealthCard card, final String pinType) {
        LOG.debug("pinType: " + pinType);
        final Map<String, PinConfiguration> container = getContainer(card);
        final Optional<String> optional = container.keySet().stream().filter(k -> k.endsWith(pinType)).findAny();
        if (optional.isPresent()) {
            final String key = optional.get();
            return container.get(key);
        }
        throw new HealthcardCommonRuntimeException(
                "Not found pinType '" + pinType + "'\nValid pinType are following: " + getValidPinTypes(card) + " for card " + CardFunction.getCardType(card));
    }

    /**
     * get all valid pinType for card
     * @param card
     * @return
     */
    private static String getValidPinTypes(final IHealthCard card) {
        final Map<String, PinConfiguration> container = getContainer(card);
        final List<String> collect = container.keySet().stream().map(k -> {
            int i = k.lastIndexOf(".");
            String s = k.substring(0, i);
            int j = s.lastIndexOf(".");
            return s.substring(j + 1) + "." + k.substring(i + 1);
        }).collect(Collectors.toList());
        return collect.toString();
    }

}
