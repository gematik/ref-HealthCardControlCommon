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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gematik.ti.openhealthcard.events.request.RequestPaceKeyEvent;

/**
 * The singleton TrustedChannelPaceKeyRequestHandler subscribe to EventBus for PaceKey-Request-Events. For each PaceKeyRequest-Event start this instance a thread to
 * request the CardAccessNumber over Event-Bus request from UI or other application. After CardAccessNumber response starts the PaceKey negotiation and after success
 * negotiation would the requester informed about the pacekey.
 **/
public final class TrustedChannelPaceKeyRequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TrustedChannelPaceKeyRequestHandler.class);
    private static final String TAG = "TrustedChannelPaceKeyRequestHandler: ";

    private static TrustedChannelPaceKeyRequestHandler instance;

    private TrustedChannelPaceKeyRequestHandler() {
    }

    /**
     * Get the singleton instance
     * @return TrustedChannelPaceKeyRequestHandler instance
     */
    public static TrustedChannelPaceKeyRequestHandler getInstance() {
        if (instance == null) {
            instance = new TrustedChannelPaceKeyRequestHandler();
        }
        return instance;
    }

    /**
     * Start the handling of paceKey request Events
     */
    public static void startHandling() {
        getInstance().register();
    }

    /**
     * Stop the handling of paceKey request Events
     */
    public static void stopHandling() {
        getInstance().unregister();
    }

    private void register() {
        EventBus.getDefault().register(this);
        LOG.debug(TAG + "registered");
    }

    private void unregister() {
        EventBus.getDefault().unregister(this);
        LOG.debug(TAG + "unregistered");
    }

    /**
     * Method that handles the RequestPaceKey Events and trigger the negotiation steps
     * @param requestPaceKeyEvent Event for pace key request
     */
    @Subscribe
    public void handleRequestPaceKeyEvent(final RequestPaceKeyEvent requestPaceKeyEvent) {
        new TrustedChannelConstructor(requestPaceKeyEvent).start();
    }
}
