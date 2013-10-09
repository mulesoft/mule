/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


