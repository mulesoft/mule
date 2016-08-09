/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.routing.UntilSuccessful;

public class EndpointDlqUntilSuccessful extends UntilSuccessful {

  @Override
  protected void resolveDlqMessageProcessor() throws InitialisationException {
    if (deadLetterQueue instanceof EndpointBuilder) {
      try {

        dlqMP = ((EndpointBuilder) deadLetterQueue).buildOutboundEndpoint();
      } catch (final EndpointException ee) {
        throw new InitialisationException(MessageFactory
            .createStaticMessage("deadLetterQueue-ref is not a valid endpoint builder: " + deadLetterQueue), ee, this);
      }
    } else {
      super.resolveDlqMessageProcessor();
    }
  }

}
