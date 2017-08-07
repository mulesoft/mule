/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.connector.DefaultReplyToHandler;
import org.mule.runtime.core.privileged.routing.requestreply.AbstractReplyToPropertyRequestReplyReplier;

import java.util.Optional;

public class AsyncReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier {

  private MessageExchangePattern messageExchangePattern = REQUEST_RESPONSE;

  public AsyncReplyToPropertyRequestReplyReplier(Optional<MessageExchangePattern> messageExchangePatternOptional) {
    super();
    this.messageExchangePattern = messageExchangePatternOptional.orElse(messageExchangePattern);
  }

  @Override
  protected boolean shouldProcessEvent(Event event) {
    return !messageExchangePattern.hasResponse() && event.getReplyToHandler() instanceof DefaultReplyToHandler;
  }

}
