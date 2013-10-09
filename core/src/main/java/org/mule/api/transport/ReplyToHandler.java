/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo endpointUri is
 * set on the message
 */

public interface ReplyToHandler
{
    void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException;

}
