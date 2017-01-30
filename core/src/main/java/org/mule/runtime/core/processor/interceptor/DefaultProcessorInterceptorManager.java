/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.core.api.interception.ProcessorInterceptorProvider;

import java.util.ArrayList;
import java.util.List;

//TODO MULE-11521 Define if this will remain here
public class DefaultProcessorInterceptorManager implements ProcessorInterceptorProvider {

  private List<ProcessorInterceptor> interceptors = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInterceptor(ProcessorInterceptor interceptor) {
    checkNotNull(interceptor, "interceptionHandler cannot be null");

    this.interceptors.add(interceptor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ProcessorInterceptor> getInterceptors() {
    return unmodifiableList(interceptors);
  }
}
