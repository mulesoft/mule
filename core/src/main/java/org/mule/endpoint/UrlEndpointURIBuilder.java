/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.util.StringUtils;

import java.net.URI;
import java.util.Properties;

/**
 * <code>UrlEndpointURIBuilder</code> is the default endpointUri strategy suitable for
 * most connectors
 */
public class UrlEndpointURIBuilder extends AbstractEndpointURIBuilder
{
    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = "";
        if (uri.getHost() != null)
        {
            // set the endpointUri to be a proper url if host and port are set
            this.address = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1)
            {
                address += ":" + uri.getPort();
            }
        }
        if (StringUtils.isNotBlank(uri.getRawPath()))
        {
            address += uri.getRawPath();
        }

        if (StringUtils.isNotBlank(uri.getRawQuery()))
        {
            address += "?" + uri.getRawQuery();
        }
    }
}
