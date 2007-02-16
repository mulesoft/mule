/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.transaction.TransactionCallback;

import javax.jms.Message;

public abstract class MessageTransactionCallback implements TransactionCallback
{
    protected final Message message;

    public MessageTransactionCallback(Message message)
    {
        this.message = message;
    }

}
