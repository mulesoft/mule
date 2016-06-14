/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.session;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.message.SessionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A session handler used to store and retrieve session information on an
 * event. The MuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property.
 */
public class SimpleSessionHandler implements SessionHandler
{
    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        return message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);
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
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, session);
    }
    
    /**
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    public String getSessionIDKey()
    {
        return "ID";
    }
}
