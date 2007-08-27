/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class JdbcMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("jdbc");

    public static Message transactionSetAutoCommitFailed()
    {
        return createMessage(BUNDLE_PATH, 1);
    }

    public static Message jndiResourceNotFound(String name)
    {
        return createMessage(BUNDLE_PATH, 2, name);
    }
}


