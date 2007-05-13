/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class XFireMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("xfire");

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
}


