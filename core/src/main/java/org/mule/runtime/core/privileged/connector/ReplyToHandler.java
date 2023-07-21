/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
