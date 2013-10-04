/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ejb.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class EjbMessages extends MessageFactory
{
    private static final EjbMessages factory = new EjbMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("ejb");

    public static Message ejbObjectMissingCreate(Object key)
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message ejbKeyRefNotValid(Object key)
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }
}


