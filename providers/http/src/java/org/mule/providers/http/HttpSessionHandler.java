/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.provider.UMOSessionHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void populateSession(UMOMessage message, UMOSession session) throws UMOException
    {
        Cookie[] cookies = (Cookie[])message.getProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                session.setProperty(cookie.getName(), cookie.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug("Added cookie to session: " + cookie.toString());
                }
            }
        }
    }

    public void writeSession(UMOMessage message, UMOSession session) throws UMOException
    {
        Object name;
        Object value;
        List cookies = new ArrayList();
        for (Iterator iterator = session.getPropertyNames(); iterator.hasNext();) {
            name = iterator.next();
            value = session.getProperty(name);
            // Todo handle domain, path, secure and exiry
            cookies.add(new Cookie(null, name.toString(), value.toString()));
        }
        if (cookies.size() > 0) {
            message.setProperty(HttpConnector.HTTP_COOKIES_PROPERTY, cookies.toArray(new Cookie[cookies
                    .size()]));
        }
    }

    public String getSessionIDKey()
    {
        return "ID";
    }
}
