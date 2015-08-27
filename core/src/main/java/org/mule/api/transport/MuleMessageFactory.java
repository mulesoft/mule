/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MuleContext;
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
    MuleMessage create(Object transportMessage, String encoding, MuleContext muleContext) throws Exception;

    /**
     * Creates a {@link MuleMessage} instance by extracting the payload from
     * <code>transportMessage</code>. Additional message properties will be taken from
     * <code>previousMessage</code>.
     */
    MuleMessage create(Object transportMessage, MuleMessage previousMessage, String encoding, MuleContext muleContext)
            throws Exception;
}
