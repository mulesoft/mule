/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

/**
 * Maintains a list of interceptors that can be applied to components.
 */
public class InterceptorStack extends AbstractInterceptingMessageProcessor implements Interceptor, Initialisable, Disposable {

  private List<Interceptor> interceptors;
  private MessageProcessorChain chain;

  public InterceptorStack() {
    // For spring
  }

  public InterceptorStack(List<Interceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return chain.process(event);
  }

  public List<Interceptor> getInterceptors() {
    return interceptors;
  }

  public void setInterceptors(List<Interceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
    chainBuilder.setName("interceptor stack");
    for (Interceptor interceptor : interceptors) {
      if (interceptor instanceof Initialisable) {
        ((Initialisable) interceptor).initialise();
      }
      chainBuilder.chain(interceptor);
    }
    if (next != null) {
      chainBuilder.chain(next);
    }
    chain = chainBuilder.build();
    chain.setMuleContext(muleContext);
    chain.setFlowConstruct(flowConstruct);
    chain.initialise();
  }

  @Override
  public void dispose() {
    for (Interceptor interceptor : interceptors) {
      if (interceptor instanceof Disposable) {
        ((Disposable) interceptor).dispose();
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((interceptors == null) ? 0 : interceptors.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    InterceptorStack other = (InterceptorStack) obj;
    if (interceptors == null) {
      if (other.interceptors != null)
        return false;
    } else if (!interceptors.equals(other.interceptors))
      return false;
    return true;
  }

  @Override
  public Processor getNext() {
    return next;
  }

}
