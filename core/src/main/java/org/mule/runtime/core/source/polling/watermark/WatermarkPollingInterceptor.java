/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.source.polling.watermark;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.source.polling.MessageProcessorPollingInterceptor;

/**
 * Implementation of {@link MessageProcessorPollingInterceptor} that retrieves the current watermark value before the polling flow
 * is executed and updates it when it is finished.
 * 
 * @since 3.5.0
 */
public class WatermarkPollingInterceptor extends MessageProcessorPollingInterceptor {

  protected final Watermark watermark;

  public WatermarkPollingInterceptor(Watermark watermark) {
    this.watermark = watermark;
  }

  /**
   * Watermark source preprocessing puts the watermark value into a flow variable
   */
  @Override
  public Event prepareSourceEvent(Event event) throws MuleException {
    return this.watermark.putInto(event);
  }

  /**
   * Watermark route preparation carries the value from the source event to the flow event
   */
  @Override
  public Event prepareRouting(Event sourceEvent, Event event, FlowConstruct flow) throws ConfigurationException {
    if (!(flow instanceof ProcessingDescriptor && ((ProcessingDescriptor) flow).isSynchronous())) {
      throw new ConfigurationException(CoreMessages.watermarkRequiresSynchronousProcessing());
    }

    String variableName = this.watermark.resolveVariable(event);
    return Event.builder(event).addVariable(variableName, sourceEvent.getVariable(variableName).getValue()).build();
  }

  /**
   * Watermark post processing saves the flow variable to the object store
   */
  @Override
  public void postProcessRouting(Event event) throws ObjectStoreException {
    this.watermark.updateFrom(event);
  }

}
