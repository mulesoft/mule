/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class DbMessages extends MessageFactory
{

    private static final DbMessages factory = new DbMessages();

    private static final String BUNDLE_PATH = getBundlePath("db");

    public static Message transactionSetAutoCommitFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }
}
