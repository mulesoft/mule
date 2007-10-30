/*
 * $Id: CxfMessages.java 6446 2007-05-10 09:33:21Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class CxfMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("cxf");

    public static Message serviceIsNull(String serviceName)
    {
        return createMessage(BUNDLE_PATH, 8, serviceName);
    }

    public static Message annotationsRequireJava5()
    {
        return createMessage(BUNDLE_PATH, 9);
    }

    public static Message couldNotInitAnnotationProcessor(Object object)
    {
        return createMessage(BUNDLE_PATH, 10, object);
    }

    public static Message unableToInitBindingProvider(String bindingProvider)
    {
        return createMessage(BUNDLE_PATH, 11, bindingProvider);
    }

    public static Message unableToLoadServiceClass(String classname)
    {
        return createMessage(BUNDLE_PATH, 12, classname);
    }

    public static Message unableToConstructAdapterForNullMessage()
    {
        return createMessage(BUNDLE_PATH, 13);
    }

    public static Message inappropriateMessageTypeForAttachments(String className)
    {
        return createMessage(BUNDLE_PATH, 14, className);
    }

    public static Message bothServiceClassAndWsdlUrlAreRequired()
    {
        return createMessage(BUNDLE_PATH, 15);
    }

    public static Message incorrectlyFormattedEndpointUri(String uri)
    {
        return createMessage(BUNDLE_PATH, 16, uri);
    }

    public static Message invalidFrontend(String frontend)
    {
        return createMessage(BUNDLE_PATH, 17, frontend);
    }
}
