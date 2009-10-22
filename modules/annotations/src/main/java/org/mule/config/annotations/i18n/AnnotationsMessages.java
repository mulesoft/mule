/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.lang.annotation.Annotation;

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
}

