/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.rmi.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class RmiMessages extends MessageFactory
{
    private static final RmiMessages factory = new RmiMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("rmi");

    public static Message messageParamServiceMethodNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message messageReceiverNeedsRmiAble()
    {
        return factory.createMessage(BUNDLE_PATH, 10);
    }

    public static Message serviceClassInvocationFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 11);
    }
}


