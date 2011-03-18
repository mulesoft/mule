/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.SessionException;
import org.mule.api.transport.SessionHandler;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.Base64;
import org.mule.util.SerializationUtils;

import java.io.IOException;

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

    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        final Object cookiesObject = message.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        final String cookieName = MuleProperties.MULE_SESSION_PROPERTY;
        final String cookieValue = CookieHelper.getCookieValueFromCookies(cookiesObject, cookieName);

        MuleSession session = null;

        if (cookieValue != null)
        {
            byte[] serializedSession = Base64.decode(cookieValue);
            
            if (serializedSession != null)
            {
                session = (MuleSession) SerializationUtils.deserialize(serializedSession, message.getMuleContext());
            }
        }
        return session;
    }


    /**
     * @deprecated Use retrieveSessionInfoFromMessage(MuleMessage message) instead
     */
    @Deprecated
    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        session = retrieveSessionInfoFromMessage(message);
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        byte[] serializedSession = SerializationUtils.serialize(session);
        String serializedEncodedSession;
        try
        {
            serializedEncodedSession = Base64.encodeBytes(serializedSession, Base64.DONT_BREAK_LINES);
        }
        catch (IOException e)
        {
            throw new SessionException(MessageFactory.createStaticMessage("Unable to serialize MuleSession"), e);
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding serialized and base64-encoded Session header to message: " + serializedEncodedSession);
        }

        final Object preExistentCookies = message.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        final String cookieName = MuleProperties.MULE_SESSION_PROPERTY;
        final String cookieValue = serializedEncodedSession;

        Object mergedCookies = CookieHelper.putAndMergeCookie(preExistentCookies, cookieName, cookieValue);

        message.setOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY, mergedCookies);
    }



    /**
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    @Deprecated
    public String getSessionIDKey()
    {
        return "ID";
    }
}
