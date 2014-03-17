/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

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
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A session handler used to store and retrieve session information on an
 * event. The MuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as String key/value
 * pairs that are Base64 encoded, for example:
 * ID=dfokokdf-3ek3oke-dkfokd;MySessionProp1=Value1;MySessionProp2=Value2
 * 
 * @deprecated Since all properties are converted to Strings, this session handler has the issue EE-1705/MULE-4567.  Use {@link SerializeAndEncodeSessionHandler} or {@link SerializeOnlySessionHandler} instead.
 */
public class LegacySessionHandler implements SessionHandler
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private static Transformer encoder = new Base64Encoder();
    private static Transformer decoder = new Base64Decoder();

    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
         MuleSession session = new DefaultMuleSession();

         String sessionId = message.getInboundProperty(MuleProperties.MULE_SESSION_ID_PROPERTY);
         Object sessionHeader = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);

         if (sessionId != null)
         {
             throw new IllegalStateException(
                 "This session handler does not know how to look up session information for session id: "
                                 + sessionId);
         }
         if (sessionHeader != null)
         {
             String sessionString;
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
         return session;
   }

    /**
     * @deprecated Use retrieveSessionInfoFromMessage(MuleMessage message) instead
     */
    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        session = retrieveSessionInfoFromMessage(message);
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        StringBuilder buf = new StringBuilder();
        buf.append(getSessionIDKey()).append("=").append(session.getId());
        for (String key : session.getPropertyNamesAsSet())
        {
            buf.append(";");
            buf.append(key).append("=").append(session.getProperty(key));
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Adding property to session header: %s=%s", key, session.getProperty(key)));
            }
        }
        String sessionString = buf.toString();
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding session header to message: " + sessionString);
        }
        sessionString = (String) encoder.transform(sessionString);
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, sessionString);
    }
    
    /**
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    public String getSessionIDKey()
    {
        return "ID";
    }
}
