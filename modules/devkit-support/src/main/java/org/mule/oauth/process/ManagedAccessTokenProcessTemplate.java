/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.oauth.process;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.ProcessInterceptor;
import org.mule.api.ProcessTemplate;
import org.mule.api.callback.ManagedAccessTokenProcessInterceptor;
import org.mule.api.callback.ProcessCallback;
import org.mule.api.callback.ProcessCallbackProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.common.security.oauth.OAuthAdapter;
import org.mule.common.security.oauth.OAuthManager;

public class ManagedAccessTokenProcessTemplate<P> implements ProcessTemplate<P, OAuthAdapter>
{

    private final ProcessInterceptor<P, OAuthAdapter> processInterceptor;

    public ManagedAccessTokenProcessTemplate(OAuthManager<OAuthAdapter> oauthManager, MuleContext muleContext)
    {
        ProcessInterceptor<P, OAuthAdapter> processCallbackProcessInterceptor = new ProcessCallbackProcessInterceptor<P, OAuthAdapter>();
        ProcessInterceptor<P, OAuthAdapter> refreshTokenProcessInterceptor = new RefreshTokenProcessInterceptor(
            processCallbackProcessInterceptor);
        ProcessInterceptor<P, OAuthAdapter> managedAccessTokenProcessInterceptor = new ManagedAccessTokenProcessInterceptor<P>(
            refreshTokenProcessInterceptor, oauthManager, muleContext);
        processInterceptor = managedAccessTokenProcessInterceptor;
    }

    public P execute(ProcessCallback<P, OAuthAdapter> processCallback,
                     MessageProcessor messageProcessor,
                     MuleEvent event) throws Exception
    {
        return processInterceptor.execute(processCallback, null, messageProcessor, event);
    }

    public P execute(ProcessCallback<P, OAuthAdapter> processCallback, Filter filter, MuleMessage message)
        throws Exception
    {
        return processInterceptor.execute(processCallback, null, filter, message);
    }

}
