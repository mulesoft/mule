/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class AxisMessages extends MessageFactory
{
    private static final AxisMessages factory = new AxisMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("axis");

    public static Message objectMustImplementAnInterface(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 1, name);
    }

    public static String serverProviderAndServerConfigConfigured()
    {
        return factory.getString(BUNDLE_PATH, 2);
    }

    public static String clientProviderAndClientConfigConfigured()
    {
        return factory.getString(BUNDLE_PATH, 3);
    }
    
    public static Message cannotInvokeCallWithoutOperation()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }
    
    public static Message couldNotFindSoapProvider(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 5, name);
    }
}


