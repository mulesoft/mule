/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


