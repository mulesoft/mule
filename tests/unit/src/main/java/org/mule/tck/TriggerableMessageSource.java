/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck;

import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.core.internal.util.rx.Operators;

import org.reactivestreams.Publisher;

public class TriggerableMessageSource extends AbstractComponent implements MessageSource {

  protected Processor listener;
  private BackPressureStrategy backPressureStrategy = WAIT;

  public TriggerableMessageSource() {}

  public TriggerableMessageSource(BackPressureStrategy backPressureStrategy) {
    this.backPressureStrategy = backPressureStrategy;
  }

  public CoreEvent trigger(CoreEvent event) throws MuleException {
    return listener.process(event);
  }

  public Publisher<CoreEvent> triggerAsync(CoreEvent event) {
    return just(event).transform(listener);
  }

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  public Processor getListener() {
    return this.listener;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  @Override
  public BackPressureStrategy getBackPressureStrategy() {
    return backPressureStrategy;
  }
}
