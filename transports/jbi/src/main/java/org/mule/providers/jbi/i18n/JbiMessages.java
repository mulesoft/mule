/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi.i18n;

import org.mule.config.i18n.MessageFactory;
import org.mule.util.StringMessageUtils;

public class JbiMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("jbi");

    public static Object receiverMustBeSet(String name)
    {
        return createMessage(BUNDLE_PATH, 1, name);
    }

    public static Object invalidReceiverType(String name, Class class1)
    {
        return createMessage(BUNDLE_PATH, 2, name, StringMessageUtils.toString(class1));
    }
}
