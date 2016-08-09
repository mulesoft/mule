/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;

public class FailingRouter extends AbstractFilteringMessageProcessor {

  @Override
  protected boolean accept(MuleEvent event) {
    throw new MuleRuntimeException(MessageFactory.createStaticMessage("Failure"));
  }
}
