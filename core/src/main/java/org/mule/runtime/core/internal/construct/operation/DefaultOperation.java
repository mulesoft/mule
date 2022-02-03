/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct.operation;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

public class DefaultOperation implements Operation {

  private MessageProcessorChain chain;

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return chain.apply(publisher);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return chain.process(event);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void initialise() throws InitialisationException {

  }

  @Override
  public void start() throws MuleException {

  }

  @Override
  public void stop() throws MuleException {

  }

  @Override
  public OperationModel getModel() {
    return null;
  }
}
