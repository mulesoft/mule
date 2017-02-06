/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.interception.ProcessorInterceptorProvider;

import java.util.ArrayList;
import java.util.List;

//TODO MULE-11521 Define if this will remain here
public class DefaultProcessorInterceptorManager implements ProcessorInterceptorProvider {

  private List<ProcessorInterceptorFactory> interceptorFactories = new ArrayList<>();
  private List<String> interceptorsOrder = new ArrayList<>();

  @Override
  public void setInterceptorsOrder(String... packagesOrder) {
    interceptorsOrder = asList(packagesOrder);
  }

  @Override
  public void addInterceptorFactory(ProcessorInterceptorFactory interceptor) {
    checkNotNull(interceptor, "interceptor cannot be null");

    this.interceptorFactories.add(interceptor);
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
