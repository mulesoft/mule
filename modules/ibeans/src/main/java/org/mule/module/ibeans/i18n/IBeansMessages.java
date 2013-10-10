/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
