/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.endpoint.ResourceNameEndpointURIBuilder;
import org.mule.api.endpoint.MalformedEndpointException;

import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * TODO
 */
public class JmsEndpointURIBuilder extends ResourceNameEndpointURIBuilder
{
    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        super.setEndpoint(uri, props);

        String newUri = null;
        if (uri.getScheme().equals("topic"))
        {
            props.setProperty(RESOURCE_INFO_PROPERTY, "topic");
            newUri = uri.toString().replace("topic://", "jms://");
        }
        else if (uri.getScheme().equals("queue"))
        {
            newUri = uri.toString().replace("queue://", "jms://");
        }

        try
        {
            if (newUri != null)
            {
                rewriteURI(new URI(newUri));
            }
        }
        catch (URISyntaxException e)
        {
            throw new MalformedEndpointException(e);
        }

    }
}
