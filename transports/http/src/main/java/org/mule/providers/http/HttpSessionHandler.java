/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.provider.UMOSessionHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will read and write Http Cookie information to and from the Mule Session
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpSessionHandler implements UMOSessionHandler
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public void retrieveSessionInfoFromMessage(UMOMessage message, UMOSession session) throws UMOException
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

    public void storeSessionInfoToMessage(UMOSession session, UMOMessage message) throws UMOException
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
