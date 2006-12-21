/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.codec.Base64Decoder;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.transformer.UMOTransformer;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A default session handler used to store and retrieve session information on an
 * event. The MuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the UMOMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as String key/value
 * pairs that are Base64 encoded, for example:
 * ID=dfokokdf-3ek3oke-dkfokd;MySessionProp1=Value1;MySessionProp2=Value2
 */
public class MuleSessionHandler implements UMOSessionHandler
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private static UMOTransformer encoder = new Base64Encoder();
    private static UMOTransformer decoder = new Base64Decoder();

    public void retrieveSessionInfoFromMessage(UMOMessage message, UMOSession session) throws UMOException
    {
        String sessionId = (String)message.removeProperty(MuleProperties.MULE_SESSION_ID_PROPERTY);
        Object sessionHeader = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);

        if (sessionId != null)
        {
            // TODO Mule 2.0 grab session from the context
            throw new IllegalStateException(
                "This session handler does not know how to look up session information for session id: "
                                + sessionId);
        }
        if (sessionHeader != null)
        {
            String sessionString = null;
            try
            {
                sessionString = new String((byte[])decoder.transform(sessionHeader), message.getEncoding());
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
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
                    throw new IllegalArgumentException(new Message(Messages.SESSION_VALUE_X_IS_MALFORMED,
                        pair).toString());
                }
                name = pair.substring(0, i).trim();
                value = pair.substring(i + 1).trim();
                session.setProperty(name, value);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Added Session variable: " + pair);
                }
            }

        }
    }

    public void storeSessionInfoToMessage(UMOSession session, UMOMessage message) throws UMOException
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
        sessionString = (String)encoder.transform(sessionString);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, sessionString);
    }

    public String getSessionIDKey()
    {
        return "ID";
    }
}
