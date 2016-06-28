/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.transport;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;

import java.nio.charset.Charset;

/**
 * <code>MuleMessageFactory</code> is a factory for creating a {@link MuleMessage} from a transport's native message
 * format (e.g. JMS message).
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface MuleMessageFactory
{

    /**
     * Creates a {@link MuleMessage} instance from <code>transportMessage</code> by extracting
     * its payload and, if available, any relevant message properties and attachments.
     */
    MutableMuleMessage create(Object transportMessage, Charset encoding, MuleContext muleContext) throws Exception;

    /**
     * Creates a {@link MuleMessage} instance by extracting the payload from
     * <code>transportMessage</code>. Additional message properties will be taken from
     * <code>previousMessage</code>.
     */
    MutableMuleMessage create(Object transportMessage, MuleMessage previousMessage, Charset encoding, MuleContext muleContext)
            throws Exception;
}
