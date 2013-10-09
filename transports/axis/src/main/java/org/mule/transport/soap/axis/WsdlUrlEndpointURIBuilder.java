/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.AbstractEndpointURIBuilder;

import java.net.URI;
import java.util.Properties;

/**
 * The same as the UrlEndpointbuilder except that all parameters except the first are
 * set as properties on the endpoint and stripped from the endpoint Uri
 */
public class WsdlUrlEndpointURIBuilder extends AbstractEndpointURIBuilder
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
        if (uri.getPath() != null)
        {
            address += uri.getPath();
        }
        String query = uri.getQuery();
        if (query != null)
        {
            int i = query.indexOf("&");
            if (i > -1)
            {
                address += "?" + query.substring(0, i);

            }
            else
            {
                address += "?" + query;
            }
        }
    }
}
