/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class SpringMessages extends MessageFactory
{
    private static final SpringMessages factory = new SpringMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("spring");

    public static Message failedToReinitMule()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message beanNotInstanceOfApplicationListener(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 12, name);
    }
}


