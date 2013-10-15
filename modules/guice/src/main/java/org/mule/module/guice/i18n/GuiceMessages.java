/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
