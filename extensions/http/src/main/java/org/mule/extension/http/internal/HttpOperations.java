/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Optional;

import javax.inject.Inject;

/**
 * General HTTP operations that do not required any specific configuration or connection.
 *
 * @since 4.0
 */
public class HttpOperations
{
    @Inject
    private MuleContext muleContext;

    /**
     * Authenticates received HTTP requests. Must be used after a listener component, setup with an error-response-builder
     * that takes flow variables as statusCode and headersRef.
     *
     * @param realm Authentication realm.
     * @param securityProviders The delegate-security-provider to use for authenticating. Use this in case you have multiple security managers defined in your configuration.
     * @param statusCodeFlowVar Reference to the flow variable name used for the statusCode attribute of the error-response-builder.
     * @param headersFlowVar Reference to the flow variable name used for the headersRef attribute of the error-response-builder.
     * @throws MuleException if unauthenticated.
     */
    public void basicSecurityFilter(String realm,
                                           @Optional String securityProviders,
                                           MuleEvent event,
                                           @Optional(defaultValue = "statusCode") String statusCodeFlowVar,
                                           @Optional(defaultValue = "headers") String headersFlowVar) throws MuleException
    {
        HttpBasicAuthenticationFilter filter = createFilter(realm, securityProviders, statusCodeFlowVar, headersFlowVar);

        filter.doFilter(event);
    }

    private HttpBasicAuthenticationFilter createFilter(String realm, String securityProviders, String statusCodeFlowVar, String headersFlowVar) throws InitialisationException
    {
        HttpBasicAuthenticationFilter filter = new HttpBasicAuthenticationFilter(statusCodeFlowVar, headersFlowVar);
        filter.setRealm(realm);
        filter.setSecurityProviders(securityProviders);
        filter.setMuleContext(muleContext);
        filter.initialise();
        return filter;
    }
}
