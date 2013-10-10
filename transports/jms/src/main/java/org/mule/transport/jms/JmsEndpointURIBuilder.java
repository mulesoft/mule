/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
