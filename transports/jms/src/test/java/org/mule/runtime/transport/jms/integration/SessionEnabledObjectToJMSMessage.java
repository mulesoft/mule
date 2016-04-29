/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import org.mule.runtime.transport.jms.transformers.ObjectToJMSMessage;

import javax.jms.Session;

/**
 * This class overrides getSession() to return the specified test MuleSession;
 * otherwise we would need a full-fledged JMS connector with dispatchers etc.
 * 
 * TODO check if we really need this stateful transformer now
 */
public class SessionEnabledObjectToJMSMessage extends ObjectToJMSMessage
{
    private final Session transformerSession;

    public SessionEnabledObjectToJMSMessage(Session session)
    {
        super();
        transformerSession = session;
    }

    @Override
    protected Session getSession()
    {
        return transformerSession;
    }
}

