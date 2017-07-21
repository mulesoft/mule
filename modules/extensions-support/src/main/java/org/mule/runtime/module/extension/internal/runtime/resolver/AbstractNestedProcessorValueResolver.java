/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.processor.chain.NestedProcessorChain;
import org.mule.runtime.core.api.util.ObjectNameHelper;

/**
 * Base class for a {@link ValueResolver} which needs to create instances of {@link NestedProcessor}, regardless of it being the
 * main return type or not
 *
 * @param <T> the generic type of the values that {@code this} instance produces
 */
abstract class AbstractNestedProcessorValueResolver<T> implements ValueResolver<T> {

  /**
   * Creates and registers a {@link NestedProcessor} that wraps the given {@code messageProcessor}
   * 
   * @param messageProcessor a {@link Processor}
   * @param event a {@link Event}
   * @param muleContext the Mule node.
   * @return a {@link NestedProcessor}
   */
  protected NestedProcessor toNestedProcessor(Processor messageProcessor, Event event, MuleContext muleContext) {
    try {
      muleContext.getRegistry().registerObject(new ObjectNameHelper(muleContext).getUniqueName(""), messageProcessor);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not register nested operation message processor"), e);
    }
    return new NestedProcessorChain(event, messageProcessor);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
