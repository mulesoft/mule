/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import java.io.IOException;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.ssl.SSLConnectionContext;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;

/**
 * Custom SSL filter that configures additional properties in the grizzly context.
 */
public class MuleSslFilter extends SSLFilter
{

    public static final String SSL_SESSION_ATTRIBUTE_KEY = "muleSslSession";

    public MuleSslFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator)
    {
        super(serverSSLEngineConfigurator, clientSSLEngineConfigurator);
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException
    {
        ctx.getAttributes().setAttribute(HTTPS.getScheme(), true);
        NextAction nextAction = super.handleRead(ctx);
        ctx.getAttributes().setAttribute(SSL_SESSION_ATTRIBUTE_KEY, getSslSession(ctx));
        return nextAction;
    }

    private SSLSession getSslSession(FilterChainContext ctx) throws SSLPeerUnverifiedException
    {
        SSLConnectionContext sslConnectionContext = obtainSslConnectionContext(ctx.getConnection());
        if (sslConnectionContext == null)
        {
            return null;
        }
        return sslConnectionContext.getSslEngine().getSession();
    }

}
