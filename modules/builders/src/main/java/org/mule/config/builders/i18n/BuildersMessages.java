/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class BuildersMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("builders");

    public static Message failedToParseConfigResource(String description)
    {
        return createMessage(BUNDLE_PATH, 1, description);
    }

    public static Message propertyTemplateMalformed(String string)
    {
        return createMessage(BUNDLE_PATH, 2, string);
    }

    public static Message systemPropertyNotSet(String property)
    {
        return createMessage(BUNDLE_PATH, 3, property);
    }

    public static Message mustSpecifyContainerRefOrClassAttribute(String containerAttrib, 
        String refAttrib, String config)
    {
        return createMessage(BUNDLE_PATH, 4, containerAttrib, refAttrib, config);
    }
}


