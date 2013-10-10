/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
