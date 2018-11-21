/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import java.io.IOException;
import java.io.InvalidClassException;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.SessionException;
import org.mule.api.serialization.SerializationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.Base64;

/**
 * A session handler used to store and retrieve session information on an
 * event. The DefaultMuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as Base64 encoded
 * byte array.
 */
public class SerializeAndEncodeSessionHandler extends SerializeOnlySessionHandler
{
    @Override
    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        MuleSession session = null;
        String serializedEncodedSession = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);
        
        if (serializedEncodedSession != null)
        {
            byte[] serializedSession = Base64.decodeWithoutUnzipping(serializedEncodedSession);
            if (serializedSession != null)
            {
                try
                {
                    session = deserialize(message, serializedSession);
                }
                catch (SerializationException e)
                {
                    Throwable rootCause = getRootCause(e);
                    
                    if (rootCause != null && rootCause instanceof InvalidClassException)
                    {
                        logger.warn("Session could not be deserialized due to class incompatibility: " + e.getCause().getMessage());
                        session = null;
                    }
                    else
                    {
                        throw e;
                    }
                }

            }
        }
        return session;
    }

    @Override
    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {        
        session = removeNonSerializableProperties(session, message.getMuleContext());
        byte[] serializedSession = serialize(message, session);

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
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, serializedEncodedSession);
    }
}
