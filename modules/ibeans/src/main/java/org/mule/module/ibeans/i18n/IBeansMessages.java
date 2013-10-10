/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.lang.reflect.Method;

public class IBeansMessages extends MessageFactory
{
    private static final IBeansMessages FACTORY = new IBeansMessages();

    private static final String BUNDLE_PATH = getBundlePath("ibeans");

    public static Message illegalCallMethod(Method method)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 6, method);
    }
}
