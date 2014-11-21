/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

/**
 * HTTP global configuration.
 */
public class HttpConfiguration
{

    public static final String USE_HTTP_TRANSPORT_FOR_URIS = MuleProperties.SYSTEM_PROPERTY_PREFIX + "http.useTransportForUris";
    private boolean useTransportForUris;

    public void setUseTransportForUris(boolean useTransportForUris)
    {
        this.useTransportForUris = useTransportForUris;
    }

    /**
     * For the cases where HTTP needs to be used by default (no connector or endpoint is explicitly defined), this
     * determines if the old HTTP transport should be used by default. If false, the new HTTP connector will be used.
     * This applies, for example, when specifying an HTTP URI in MuleClient.
     */
    private boolean useTransportForUris()
    {
        return useTransportForUris;
    }

    /**
     * @param muleContext mule context for the app.
     * @return true if the http transport must be use to process URIs, false if the new module must be used.
     */
    public static boolean useTransportForUris(final MuleContext muleContext)
    {
        final HttpConfiguration httpConfig = muleContext.getConfiguration().getExtension(HttpConfiguration.class);
        if (httpConfig != null)
        {
            return httpConfig.useTransportForUris();
        }
        else
        {
            return Boolean.getBoolean(USE_HTTP_TRANSPORT_FOR_URIS);
        }
    }
}
