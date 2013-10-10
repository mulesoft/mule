/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.acegi.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class AcegiMessages extends MessageFactory
{
    private static final AcegiMessages factory = new AcegiMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("acegi");

    public static Message basicFilterCannotHandleHeader(String header)
    {
        return factory.createMessage(BUNDLE_PATH, 1, header);
    }

    public static Message authRealmMustBeSetOnFilter()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }
}


