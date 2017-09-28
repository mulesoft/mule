/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.interception.ProcessorInterceptorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

public class DefaultProcessorInterceptorManager implements ProcessorInterceptorManager, Initialisable {

  @Inject
  private MuleContext context;

  private List<ProcessorInterceptorFactory> interceptorFactories = new ArrayList<>();
  private List<String> interceptorsOrder = new ArrayList<>();

  @Override
  @Inject
  @Named(INTERCEPTORS_ORDER_REGISTRY_KEY)
  public void setInterceptorsOrder(Optional<ProcessorInterceptorOrder> packagesOrder) {
    interceptorsOrder = packagesOrder.map(order -> order.get()).orElse(emptyList());
  }

  @Override
  public void initialise() throws InitialisationException {
    interceptorFactories.forEach(interceptorFactory -> {
      try {
        context.getInjector().inject(interceptorFactory);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

  @Override
  @Inject
  public void setInterceptorFactories(Optional<List<ProcessorInterceptorFactory>> interceptorFactories) {
    this.interceptorFactories = interceptorFactories.orElse(emptyList());
  }

  @Override
  public List<ProcessorInterceptorFactory> getInterceptorFactories() {
    final List<ProcessorInterceptorFactory> sortedInterceptors = new ArrayList<>(interceptorFactories);

    sortedInterceptors.sort((o1, o2) -> orderIndexOf(o1) - orderIndexOf(o2));

    return unmodifiableList(sortedInterceptors);
  }

  private int orderIndexOf(ProcessorInterceptorFactory factory) {
    int i = 0;
    for (String interceptorsOrderItem : interceptorsOrder) {
      if (factory.getClass().getName().startsWith(interceptorsOrderItem)) {
        return i;
      }
      ++i;
    }
    return MAX_VALUE;
  }
}
