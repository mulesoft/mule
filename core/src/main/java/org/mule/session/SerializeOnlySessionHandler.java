/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.session;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.SessionHandler;
import org.mule.util.SerializationUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A session handler used to store and retrieve session information on an
 * event. The MuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as an array of bytes (byte[])
 */
public class SerializeOnlySessionHandler implements SessionHandler
{
    protected transient Log logger = LogFactory.getLog(getClass());

    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        MuleSession session = null;
        byte[] serializedSession = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);

        if (serializedSession != null)
        {
            session = (MuleSession) SerializationUtils.deserialize(serializedSession, message.getMuleContext());
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
        byte[] serializedSession = SerializationUtils.serialize(removeNonSerializableProperties(session,message.getMuleContext()));
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding serialized Session header to message: " + serializedSession);
        }
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, serializedSession);
    }
    
    protected MuleSession removeNonSerializableProperties(final MuleSession session,
                                                          final MuleContext muleContext)
    {
        DefaultMuleSession copy = new DefaultMuleSession(session);
        copy.removeNonSerializableProperties();
        return copy;
    }
    
    /**
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    public String getSessionIDKey()
    {
        return "ID";
    }
}
