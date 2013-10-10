/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.i18n;

import org.mule.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Internationalized messages specific to the annotations module
 */
public class AnnotationsMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("annotations");

    private static final AnnotationsMessages factory = new AnnotationsMessages();

    public static Message serviceDoesNotHaveAnnotation(Class serviceClass, String annotationName)
    {
        return factory.createMessage(BUNDLE_PATH, 1, serviceClass.getName(), annotationName);
    }

    public static Message serviceHasNoEntrypoint(Class serviceClass)
    {
        return factory.createMessage(BUNDLE_PATH, 2, serviceClass.getName());
    }

    public static Message noParserFoundForAnnotation(Annotation annotation)
    {
        return factory.createMessage(BUNDLE_PATH, 3, annotation);
    }

    public static Message noPropertyConverterForType(Class type)
    {
        return factory.createMessage(BUNDLE_PATH, 4, type);
    }

    public static Message failedToInvokeReplyMethod(String method)
    {
        return factory.createMessage(BUNDLE_PATH, 5, method);
    }

    public static Message transformerMethodNotValid(Method method)
    {
        return factory.createMessage(BUNDLE_PATH, 6, method);
    }

    public static Message lookupNotFoundInRegistry(Class type, String name, Class object)
    {
        return factory.createMessage(BUNDLE_PATH, 7, type, (StringUtils.isBlank(name) ? "[no name]" : name), object);
    }

    public static Message lookupFailedSeePreviousException(Object object)
    {
        return factory.createMessage(BUNDLE_PATH, 8, object);
    }
}

