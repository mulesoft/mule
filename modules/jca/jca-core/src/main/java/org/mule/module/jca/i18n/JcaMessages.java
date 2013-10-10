/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


