/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.vm.VMConnector;

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


