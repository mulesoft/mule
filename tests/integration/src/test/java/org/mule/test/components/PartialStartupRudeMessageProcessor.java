/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class PartialStartupRudeMessageProcessor implements Processor, Startable {

  @Override
  public Event process(Event event) throws MuleException {
    return event;
  }

  @Override
  public void start() throws MuleException {
    throw new MuleException(I18nMessageFactory.createStaticMessage("TOO RUDE!")) {};
  }

}
