/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.transport.SessionHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will read and write Http Cookie information to and from the Mule MuleSession
 */
public class HttpSessionHandler implements SessionHandler
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        Cookie[] cookies = (Cookie[])message.getProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookies != null)
        {
            for (int i = 0; i < cookies.length; i++)
            {
                Cookie cookie = cookies[i];
                session.setProperty(cookie.getName(), cookie.getValue());
                if (logger.isDebugEnabled())
                {
                    logger.debug("Added cookie to session: " + cookie.toString());
                }
            }
        }
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        Object name;
        Object value;
        List cookies = new ArrayList();
        for (Iterator iterator = session.getPropertyNames(); iterator.hasNext();)
        {
            name = iterator.next();
            value = session.getProperty(name);
            // TODO handle domain, path, secure (https) and expiry
            cookies.add(new Cookie(null, name.toString(), value.toString()));
        }
        if (cookies.size() > 0)
        {
            message.setProperty(HttpConnector.HTTP_COOKIES_PROPERTY,
                cookies.toArray(new Cookie[cookies.size()]));
        }
    }

    public String getSessionIDKey()
    {
        return "ID";
    }
}
