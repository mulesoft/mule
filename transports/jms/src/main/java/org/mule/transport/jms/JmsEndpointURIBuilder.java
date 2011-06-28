/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
		
		String oldUri = uri.toString();
		
		if (oldUri.startsWith("jms://queue") || oldUri.startsWith("jms://topic")) 
		{
			oldUri = oldUri.substring(6);
		}
		
        if (oldUri.startsWith("topic"))
        {
            props.setProperty(RESOURCE_INFO_PROPERTY, "topic");
            newUri = oldUri.replace("topic://", "jms://");
        }
        else if (oldUri.startsWith("queue"))
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
