/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

import org.mule.impl.endpoint.AbstractEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * The same as the UrlEndpointbuilder except that all parameters except the first are
 * set as properties on the endpoint and stripped from the endpoint Uri
 */
public class WsdlUrlEndpointBuilder extends AbstractEndpointBuilder
{

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
