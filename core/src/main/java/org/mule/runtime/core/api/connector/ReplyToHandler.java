/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.connector;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo endpointUri is set on the message
 * 
 * @deprecated TODO MULE-9731 Migrate 3.7 {@link ReplyToHandler}-centric non-blocking support to use new non-blocking API. Move to
 *             compatibility module afterwards.
 */
@Deprecated
public interface ReplyToHandler {

  Event processReplyTo(Event event, InternalMessage returnMessage, Object replyTo) throws MuleException;

  /**
   * Processes replyTo in the case an exception occurred. Not all implementations will implement this if for example they should
   * only send a reply message in the success case.
   *
   * @param exception the exception thrown by processing
   * @param replyTo name of the channel that exception message should be sent if relevant
   */
  void processExceptionReplyTo(MessagingException exception, Object replyTo);

}
