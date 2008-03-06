/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.SessionHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.codec.Base64Decoder;
import org.mule.transformer.codec.Base64Encoder;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A default session handler used to store and retrieve session information on an
 * event. The DefaultMuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as String key/value
 * pairs that are Base64 encoded, for example:
 * ID=dfokokdf-3ek3oke-dkfokd;MySessionProp1=Value1;MySessionProp2=Value2
 */
public class MuleSessionHandler implements SessionHandler
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private static Transformer encoder = new Base64Encoder();
    private static Transformer decoder = new Base64Decoder();

    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        String sessionId = (String) message.removeProperty(MuleProperties.MULE_SESSION_ID_PROPERTY);
        Object sessionHeader = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);

        if (sessionId != null)
        {
            throw new IllegalStateException(
                "This session handler does not know how to look up session information for session id: "
                                + sessionId);
        }
        if (sessionHeader != null)
        {
            String sessionString = null;
            try
            {
                sessionString = new String((byte[]) decoder.transform(sessionHeader), message.getEncoding());
            }
            catch (UnsupportedEncodingException e)
            {
                sessionString = new String((byte[]) decoder.transform(sessionHeader));
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Parsing session header: " + sessionString);
            }
            String pair;
            String name;
            String value;
            for (StringTokenizer stringTokenizer = new StringTokenizer(sessionString, ";"); stringTokenizer.hasMoreTokens();)
            {
                pair = stringTokenizer.nextToken();
                int i = pair.indexOf("=");
                if (i == -1)
                {
                    throw new IllegalArgumentException(
                        CoreMessages.sessionValueIsMalformed(pair).toString());
                }
                name = pair.substring(0, i).trim();
                value = pair.substring(i + 1).trim();
                session.setProperty(name, value);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Added MuleSession variable: " + pair);
                }
            }

        }
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getSessionIDKey()).append("=").append(session.getId());
        for (Iterator iterator = session.getPropertyNames(); iterator.hasNext();)
        {
            Object o = iterator.next();
            buf.append(";");
            buf.append(o).append("=").append(session.getProperty(o));
            if (logger.isDebugEnabled())
            {
                logger.debug("Adding property to session header: " + o + "=" + session.getProperty(o));
            }
        }
        String sessionString = buf.toString();
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding session header to message: " + sessionString);
        }
        sessionString = (String) encoder.transform(sessionString);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, sessionString);
    }

    public String getSessionIDKey()
    {
        return "ID";
    }
}
