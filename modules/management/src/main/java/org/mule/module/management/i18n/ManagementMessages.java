/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class ManagementMessages extends MessageFactory
{
    private static final ManagementMessages factory = new ManagementMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("management");

    public static Message createOrLocateShouldBeSet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message cannotLocateOrCreateServer()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message noMBeanServerAvailable()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message forceGC(long[] heapSizes)
    {
        return factory.createMessage(BUNDLE_PATH, 4, String.valueOf(heapSizes[0]), String.valueOf(heapSizes[1]));
    }


}


