/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.builders.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class BuildersMessages extends MessageFactory
{
    private static final BuildersMessages factory = new BuildersMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("builders");

    public static Message failedToParseConfigResource(String description)
    {
        return factory.createMessage(BUNDLE_PATH, 1, description);
    }

    public static Message propertyTemplateMalformed(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 2, string);
    }

    public static Message systemPropertyNotSet(String property)
    {
        return factory.createMessage(BUNDLE_PATH, 3, property);
    }

    public static Message mustSpecifyContainerRefOrClassAttribute(String containerAttrib, 
        String refAttrib, String config)
    {
        return factory.createMessage(BUNDLE_PATH, 4, containerAttrib, refAttrib, config);
    }
}


