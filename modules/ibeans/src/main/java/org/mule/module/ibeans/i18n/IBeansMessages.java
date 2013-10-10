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
import java.util.Arrays;

public class IBeansMessages extends MessageFactory
{
    private static final IBeansMessages FACTORY = new IBeansMessages();

    private static final String BUNDLE_PATH = getBundlePath("ibeans");

    public static Message ibeanNotRegistered(String ibeanName)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 1, ibeanName);
    }

    public static Message ibeanMethodFoundButNotValid(String ibeanName, String methodName)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 2, ibeanName, methodName);
    }

    public static Message ibeanMethodNotFound(String ibeanName, String methodName)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 3, ibeanName, methodName);
    }

    public static Message ibeanMethodNotFound(String ibeanName, String methodName, Class[] parameters)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 4, methodName, Arrays.toString(parameters), ibeanName);
    }

    public static Message ibeanMethodParametersDoNotMatch(String iBeanName, String methodName, Class[] eventParams, Class[] methodParams)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 5, methodName, Arrays.toString(eventParams), methodName, iBeanName, Arrays.toString(methodParams));
    }

    public static Message illegalCallMethod(Method method)
    {
        return FACTORY.createMessage(BUNDLE_PATH, 6, method);
    }
}
