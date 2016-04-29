/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.endpoint.MalformedEndpointException;
import org.mule.runtime.core.endpoint.ResourceNameEndpointURIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * TODO
 */
public class JmsEndpointURIBuilder extends ResourceNameEndpointURIBuilder
{
    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        super.setEndpoint(uri, props);

		String oldUri = uri.toString();

        String newUri = null;
        if (uri.getScheme().equals("topic"))
        {
            props.setProperty(RESOURCE_INFO_PROPERTY, "topic");
            newUri = uri.toString().replace("topic://", "jms://");
        }
        else if (uri.getScheme().equals("queue"))
        {
            newUri = uri.toString().replace("queue://", "jms://");
        }//Added by Eugene - dynamic endpoints were resolved to jms://queue:// etc
		else if (oldUri.startsWith("jms://queue://"))
		{
			newUri = uri.toString().replace("jms://queue://", "jms://queue:");
		}
		else if (oldUri.startsWith("jms://temp-queue://"))
		{
			newUri = uri.toString().replace("jms://temp-queue://", "jms://temp-queue:");
		}
		else if (oldUri.startsWith("jms://topic://"))
		{
			props.setProperty(RESOURCE_INFO_PROPERTY, "topic");
			newUri = uri.toString().replace("jms://topic://", "jms://topic:");
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
