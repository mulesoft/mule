/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.UserInfoEndpointURIBuilder;
import org.mule.transport.xmpp.i18n.XmppMessages;

import java.net.URI;
import java.util.Properties;

/**
 * This endpoint builder ensures that a path is set on the URI as the path is the
 * message type.
 */
public class XmppEndpointURIBuilder extends UserInfoEndpointURIBuilder
{
    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        checkXmppMessageType(uri);
        checkRecipient(uri);
        super.setEndpoint(uri, props);
    }

    private void checkXmppMessageType(URI uri) throws MalformedEndpointException
    {
        // the XMPP message type is stored in the host of the URL
        String host = uri.getHost();

        if (host.length() == 0)
        {
            throw new MalformedEndpointException(XmppMessages.noMessageTypeInUri(), uri.toString());
        }

        try
        {
            XmppMessageType.valueOf(host);
        }
        catch (IllegalArgumentException e)
        {
            throw new MalformedEndpointException(XmppMessages.invalidMessageTypeInUri(), uri.toString());
        }
    }

    private void checkRecipient(URI uri) throws MalformedEndpointException
    {
        if (isDynamic(uri))
        {
            return;
        }

        // the recipient is stored in the path of the URL
        if (uri.getPath().length() == 0)
        {
            throw new MalformedEndpointException(XmppMessages.noRecipientInUri(), uri.toString());
        }
    }

    private boolean isDynamic(URI uri)
    {
        String host = uri.getHost();
        return XmppMessageType.dynamic.name().equals(host);
    }
}
