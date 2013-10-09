/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleMessage;

/**
 * <code>MuleMessageFactory</code> is a factory for creating a {@link MuleMessage} from a transport's
 * native message format (e.g. JMS message).
 */
public interface MuleMessageFactory
{
    /**
     * Creates a {@link MuleMessage} instance from <code>transportMessage</code> by extracting
     * its payload and, if available, any relevant message properties and attachments.
     */
    MuleMessage create(Object transportMessage, String encoding) throws Exception;

    /**
     * Creates a {@link MuleMessage} instance by extracting the payload from 
     * <code>transportMessage</code>. Additional message properties will be taken from
     * <code>previousMessage</code>.
     */
    MuleMessage create(Object transportMessage, MuleMessage previousMessage, String encoding)
        throws Exception;
}
