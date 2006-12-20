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

import java.net.URI;
import java.util.Properties;

import org.mule.config.i18n.Message;
import org.mule.impl.endpoint.UserInfoEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

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
            throw new MalformedEndpointException(new Message("xmpp", 1), uri.toString());
        }
        if (props.getProperty(XmppConnector.XMPP_GROUP_CHAT, "false").equalsIgnoreCase("true"))
        {
            if (props.getProperty(XmppConnector.XMPP_NICKNAME, null) == null)
            {
                throw new MalformedEndpointException(new Message("xmpp", 2), uri.toString());
            }
        }
        super.setEndpoint(uri, props);
    }
}
