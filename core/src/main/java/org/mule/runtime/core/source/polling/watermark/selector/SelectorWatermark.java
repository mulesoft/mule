/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.source.polling.watermark.selector;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.source.polling.MessageProcessorPollingInterceptor;
import org.mule.runtime.core.source.polling.watermark.Watermark;

import java.io.Serializable;

/**
 * Implementation of {@link Watermark} that relies on a {@link WatermarkSelector} to update its values
 * 
 * @since 3.5.0
 */
public class SelectorWatermark extends Watermark implements Initialisable, MuleContextAware {

  private final WatermarkSelectorBroker selectorBroker;
  private final String selectorExpression;

  public SelectorWatermark(ObjectStore<Serializable> objectStore, String variable, String defaultExpression,
                           WatermarkSelectorBroker selectorBroker, String selectorExpression) {
    super(objectStore, variable, defaultExpression);
    this.selectorBroker = selectorBroker;
    this.selectorExpression = selectorExpression;
  }

  @Override
  public void initialise() throws InitialisationException {
    ValidationResult result = muleContext.getExpressionManager().validate(selectorExpression);
    if (!result.isSuccess()) {
      throw new InitialisationException(createStaticMessage(format("selector-expression requires a valid MEL expression. '%s' was found instead",
                                                                   selectorExpression)),
                                        this);
    }
  }

  /**
   * Returns the {@link #selectorBroker} value and resets it so that its reusable. Notice that the selectorBroker is reusable
   * without risk of concurrency issues because watermark only works on synchronous flows
   */
  @Override
  protected Object getUpdatedValue(Event event) {
    // interceptor is responsible for returning the selected value
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @return a new {@link SelectorWatermarkPollingInterceptor}
   */
  @Override
  public MessageProcessorPollingInterceptor interceptor() {
    return new SelectorWatermarkPollingInterceptor(this, this.selectorBroker.newSelector(), this.selectorExpression);
  }
}
