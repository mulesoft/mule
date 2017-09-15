/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing.requestreply;

import static org.mule.runtime.core.api.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

public class ReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier {

  private FlowConstruct flowConstruct;

  @Override
  protected boolean shouldProcessEvent(PrivilegedEvent event) {
    MessageExchangePattern mep = REQUEST_RESPONSE;
    if (getFlowConstruct() instanceof Pipeline
        && ((Pipeline) getFlowConstruct()).getSource() instanceof LegacyImmutableEndpoint) {
      mep = ((LegacyImmutableEndpoint) ((Pipeline) getFlowConstruct()).getSource()).getExchangePattern();
    }
    return mep.hasResponse();
  }

  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }
}
