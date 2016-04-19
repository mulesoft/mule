/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.i18n;

import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.transport.vm.VMConnector;

public class VMMessages extends MessageFactory
{
    private static final VMMessages factory = new VMMessages();
    
    private static final String BUNDLE_PATH = getBundlePath(VMConnector.VM);

    public static Message noReceiverForEndpoint(String name, Object uri)
    {
        return factory.createMessage(BUNDLE_PATH, 1, name, uri);
    }

    public static Message queueIsFull(String queueName, int maxCapacity)
    {
        return factory.createMessage(BUNDLE_PATH, 2, queueName, maxCapacity);
    }
}


