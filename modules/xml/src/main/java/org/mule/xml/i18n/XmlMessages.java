/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.xml.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/** TODO */
public class XmlMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("xml");

    public static Message failedToProcessXPath(String expression)
    {
        return createMessage(BUNDLE_PATH, 1, expression);
    }

    public static Message domTypeNotSupported(Class type)
    {
        return createMessage(BUNDLE_PATH, 2, type);
    }
}
