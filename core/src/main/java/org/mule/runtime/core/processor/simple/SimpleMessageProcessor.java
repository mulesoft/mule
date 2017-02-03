/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Base class for {@link Processor}s that change the event or message. Implementations will return a new instance of the event
 * with the updated data, since the {@link Event} and {@link Message} objects are immutable.
 *
 * @since 4.0
 */
public abstract class SimpleMessageProcessor extends AbstractAnnotatedObject
    implements Processor, MuleContextAware, Initialisable {

  protected MuleContext muleContext;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
