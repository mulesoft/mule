/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/**
 * i18n Guice messages
 */
public class GuiceMessages extends MessageFactory
{
    private static final GuiceMessages factory = new GuiceMessages();

    private static final String BUNDLE_PATH = getBundlePath("guice");

    public static Message failedToRegisterOBjectWithMule(Class objectType)
    {
        return factory.createMessage(BUNDLE_PATH, 1, objectType.getName());
    }
}
