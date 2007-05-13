/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.mule.impl.endpoint.UserInfoEndpointBuilder;
import org.mule.providers.xmpp.i18n.XmppMessages;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * Does the same as the UserINfoEndpointBuilder but also ensures that a path is set
 * on the uri. The path is used as either the groupChat name or the recipient name of
 * a one on one chat.
 */
public class XmppEndpointBuilder extends UserInfoEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        if (uri.getPath().length() == 0)
        {
            throw new MalformedEndpointException(XmppMessages.noRecipientInUri(), uri.toString());
        }
        if (props.getProperty(XmppConnector.XMPP_GROUP_CHAT, "false").equalsIgnoreCase("true"))
        {
            if (props.getProperty(XmppConnector.XMPP_NICKNAME, null) == null)
            {
                throw new MalformedEndpointException(XmppMessages.nicknameMustBeSet(), uri.toString());
            }
        }
        super.setEndpoint(uri, props);
    }
}
