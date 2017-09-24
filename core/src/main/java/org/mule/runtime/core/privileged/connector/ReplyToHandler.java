/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.connector;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.exception.MuleException;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo endpointUri is set on the message
 * 
 * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
 */
@Deprecated
public interface ReplyToHandler {

  CoreEvent processReplyTo(CoreEvent event, Message returnMessage, Object replyTo) throws MuleException;

}
