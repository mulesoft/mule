/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class JcaMessages extends MessageFactory
{
    private static final JcaMessages factory = new JcaMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("jca");

    public static Message authDeniedOnEndpoint(Object endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 1, endpoint);
    }

    public static Message objectMarkedInvalid(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 2, string);
    }

    public static Object objectIsDisposed(Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 3, object);
    }
    
    public static Message cannotPauseResumeJcaComponent()
    {
        return factory.createMessage(BUNDLE_PATH, 4);
    }
    
    public static Message cannotAllocateManagedInstance()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }
    
    
}


