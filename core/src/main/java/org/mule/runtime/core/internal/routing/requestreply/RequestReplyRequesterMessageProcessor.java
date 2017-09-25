/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.requestreply;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Processes a {@link CoreEvent} by invoking the next {@link Processor} but receiving the reply, which is turn is returned from this
 * MessageProcessor from a seperate {@link MessageSource} rather than using the return value of the <code>next</code>
 * MessageProcessor invocation. Because two seperate channels are used, most implementations will want to implement the concept of
 * a timeout which defines how long a reply should be waited for.
 *
 * @since 3.0
 */
public interface RequestReplyRequesterMessageProcessor {

  /**
   * @param replyMessageSource the message source that will be used to receive the reply message
   */
  void setReplySource(MessageSource replyMessageSource);
}
