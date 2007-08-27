/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class TestMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("test");
    
    public static Message testMessage(String arg1, String arg2, String arg3)
    {
        return createMessage(BUNDLE_PATH, 1, arg1, arg2, arg3);
    }
}


