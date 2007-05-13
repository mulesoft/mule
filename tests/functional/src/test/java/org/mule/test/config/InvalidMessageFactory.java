/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class InvalidMessageFactory extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("thisdoesnotexist");
    
    public static Message getInvalidMessage()
    {
        // the code can safely be ignored. MessageFactory must fail before when
        // trying to find the inexistent bundle.
        return createMessage(BUNDLE_PATH, 42);
    }
}


