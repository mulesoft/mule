/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.routing.outbound.FilteringOutboundRouter;

public class ExceptionThrowingOutboundRouter extends FilteringOutboundRouter {

  public MuleEvent process(MuleEvent event) throws MuleException {
    throw new RoutingException(MessageFactory.createStaticMessage("dummyException"), event, null);
  }
}


