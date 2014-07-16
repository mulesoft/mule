/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.process;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.devkit.ProcessInterceptor;
import org.mule.api.devkit.ProcessTemplate;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.callback.ProcessCallback;

public class ManagedAccessTokenProcessTemplate<P> implements ProcessTemplate<P, OAuth2Adapter>
{

    private final ProcessInterceptor<P, OAuth2Adapter> processInterceptor;

    public ManagedAccessTokenProcessTemplate(OAuth2Manager<OAuth2Adapter> oauthManager,
                                             MuleContext muleContext)
    {
        ProcessInterceptor<P, OAuth2Adapter> processCallbackProcessInterceptor = new ProcessCallbackProcessInterceptor<P, OAuth2Adapter>();

        ProcessInterceptor<P, OAuth2Adapter> refreshTokenProcessInterceptor = (ProcessInterceptor<P, OAuth2Adapter>) new RefreshTokenProcessInterceptor<P>(
            processCallbackProcessInterceptor, muleContext);

        ProcessInterceptor<P, OAuth2Adapter> managedAccessTokenProcessInterceptor = new ManagedAccessTokenProcessInterceptor<P>(
            refreshTokenProcessInterceptor, oauthManager);
        processInterceptor = managedAccessTokenProcessInterceptor;
    }

    public P execute(ProcessCallback<P, OAuth2Adapter> processCallback,
                     MessageProcessor messageProcessor,
                     MuleEvent event) throws Exception
    {
        return processInterceptor.execute(processCallback, null, messageProcessor, event);
    }

    public P execute(ProcessCallback<P, OAuth2Adapter> processCallback, Filter filter, MuleMessage message)
        throws Exception
    {
        return processInterceptor.execute(processCallback, null, filter, message);
    }

}
