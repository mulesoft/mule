/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing.requestreply;

import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.endpoint.LegacyImmutableEndpoint;

public class ReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier {

  @Override
  protected boolean shouldProcessEvent(Event event) {
    MessageExchangePattern mep = REQUEST_RESPONSE;
    if (flowConstruct instanceof Pipeline && ((Pipeline) flowConstruct).getSource() instanceof LegacyImmutableEndpoint) {
      mep = ((LegacyImmutableEndpoint) ((Pipeline) flowConstruct).getSource()).getExchangePattern();
    }
    return mep.hasResponse();
  }

}
