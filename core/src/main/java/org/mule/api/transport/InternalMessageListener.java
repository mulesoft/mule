/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transaction.Transaction;

import java.io.OutputStream;

/**
 * A listener used to receive Muleevents from a transport receiver. The listener can be
 * swapped out to deliver message to other frameworks, bypassing the Mule container.
 */
public interface InternalMessageListener
{
    MuleMessage onMessage(MuleMessage message,
                         Transaction trans,
                         boolean synchronous,
                         OutputStream outputStream) throws MuleException;

}
