/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.oauth;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.ProcessAdapter;
import org.mule.api.ProcessTemplate;
import org.mule.api.callback.ProcessCallback;
import org.mule.api.capability.Capabilities;
import org.mule.api.capability.ModuleCapability;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.oauth.callback.RestoreAccessTokenCallback;
import org.mule.api.oauth.callback.SaveAccessTokenCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.common.security.oauth.OAuth2Connector;
import org.mule.config.MuleManifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseOAuth2Connector
    implements OAuth2Connector, Capabilities, Disposable, Initialisable, Startable, Stoppable,
    ProcessAdapter<BaseOAuth2Connector>
{

    private static final long serialVersionUID = 8010641516561465465L;

    private SaveAccessTokenCallback oauthSaveAccessToken;
    private RestoreAccessTokenCallback oauthRestoreAccessToken;

    /**
     * Returns true if this module implements such capability
     */
    public final boolean isCapableOf(ModuleCapability capability)
    {
        if (capability == ModuleCapability.LIFECYCLE_CAPABLE)
        {
            return true;
        }
        if (capability == ModuleCapability.OAUTH2_CAPABLE)
        {
            return true;
        }
        return false;
    }

    @Override
    public final void initialise() throws InitialisationException
    {
        Logger log = LoggerFactory.getLogger(BaseOAuth2Connector.class);
        String runtimeVersion = MuleManifest.getProductVersion();
        if (runtimeVersion.equals("Unknown"))
        {
            log.warn("Unknown Mule runtime version. This module may not work properly!");
        }
        else
        {
            String[] expectedMinVersion = "3.4".split("\\.");
            if (runtimeVersion.contains("-"))
            {
                runtimeVersion = runtimeVersion.split("-")[0];
            }
            String[] currentRuntimeVersion = runtimeVersion.split("\\.");
            for (int i = 0; (i < expectedMinVersion.length); i++)
            {
                try
                {
                    if (Integer.parseInt(currentRuntimeVersion[i]) > Integer.parseInt(expectedMinVersion[i]))
                    {
                        break;
                    }
                    if (Integer.parseInt(currentRuntimeVersion[i]) < Integer.parseInt(expectedMinVersion[i]))
                    {
                        throw new RuntimeException("This module requires at least Mule 3.4");
                    }
                }
                catch (NumberFormatException nfe)
                {
                    log.warn("Error parsing Mule version, cannot validate current Mule version");
                }
                catch (ArrayIndexOutOfBoundsException iobe)
                {
                    log.warn("Error parsing Mule version, cannot validate current Mule version");
                }
            }
        }
    }

    @Override
    public final void dispose()
    {
    }

    @Override
    public void start() throws MuleException
    {
    }

    @Override
    public void stop() throws MuleException
    {
    }

    public final <P> ProcessTemplate<P, BaseOAuth2Connector> getProcessTemplate()
    {
        final BaseOAuth2Connector object = this;
        return new ProcessTemplate<P, BaseOAuth2Connector>()
        {

            @Override
            public P execute(ProcessCallback<P, BaseOAuth2Connector> processCallback,
                             MessageProcessor messageProcessor,
                             MuleEvent event) throws Exception
            {
                return processCallback.process(object);
            }

            @Override
            public P execute(ProcessCallback<P, BaseOAuth2Connector> processCallback,
                             Filter filter,
                             MuleMessage message) throws Exception
            {
                return processCallback.process(object);
            }

        };
    }

    public SaveAccessTokenCallback getOauthSaveAccessToken()
    {
        return oauthSaveAccessToken;
    }

    public void setOauthSaveAccessToken(SaveAccessTokenCallback oauthSaveAccessToken)
    {
        this.oauthSaveAccessToken = oauthSaveAccessToken;
    }

    public RestoreAccessTokenCallback getOauthRestoreAccessToken()
    {
        return oauthRestoreAccessToken;
    }

    public void setOauthRestoreAccessToken(RestoreAccessTokenCallback oauthRestoreAccessToken)
    {
        this.oauthRestoreAccessToken = oauthRestoreAccessToken;
    }

}
