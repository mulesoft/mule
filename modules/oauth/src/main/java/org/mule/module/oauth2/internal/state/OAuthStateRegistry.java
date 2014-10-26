/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the OAuth states in a certain mule context.
 */
public class OAuthStateRegistry
{

    private Map<String, ConfigOAuthState> oauthStatePerConfig = new HashMap<String, ConfigOAuthState>();

    /**
     * Registers a new oauth state for a config.
     *
     * @param configName       name of the oauth config to register
     * @param configOAuthState the initial state for that config
     */
    public void registerOAuthState(final String configName, final ConfigOAuthState configOAuthState)
    {
        oauthStatePerConfig.put(configName, configOAuthState);
    }

    /**
     * Unregisters an oauth state for a config.
     *
     * @param configName name of the config to unregister
     */
    public void unregisterOAuthState(final String configName)
    {
        oauthStatePerConfig.remove(configName);
    }

    /**
     * Gets the oauth state for a particular config
     *
     * @param oauthConfigName name of the oauth state config to retrieve
     * @return null if there's no state register for that config, otherwise the oauth config state.
     */
    public ConfigOAuthState getStateForConfig(final String oauthConfigName)
    {
        return oauthStatePerConfig.get(oauthConfigName);
    }
}
