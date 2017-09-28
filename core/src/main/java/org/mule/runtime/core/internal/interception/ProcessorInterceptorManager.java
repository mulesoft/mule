/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.interception;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;

import java.util.List;
import java.util.Optional;

public interface ProcessorInterceptorManager {

  String PROCESSOR_INTERCEPTOR_MANAGER_REGISTRY_KEY = "_muleProcessorInterceptorManager";

  /**
   * Determines the order in which the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} products will be applied
   * to the applicable components.
   *
   * @see ProcessorInterceptorFactory#INTERCEPTORS_ORDER_REGISTRY_KEY
   * @param packagesOrder the wanted order for the interceptors.
   */
  void setInterceptorsOrder(Optional<ProcessorInterceptorOrder> packagesOrder);

  /**
   * Sets the {@link ProcessorInterceptorFactory}ies to be applied to the components of a flow.
   * <p>
   * By default, the created {@link ProcessorInterceptor}s will be applied in the same order as they were given to this method. To
   * change this default ordering, {@link #setInterceptorsOrder(Optional<List<String>>)} must be called with the desired order.
   *
   * @param interceptorFactories the factories of {@link ProcessorInterceptor}s to add.
   */
  void setInterceptorFactories(Optional<List<ProcessorInterceptorFactory>> interceptorFactories);

  /**
   * Provides the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} that are available in the {@link Registry}, in
   * the order defined by {@link #setInterceptorsOrder(Optional<List<String>>)} if defined.
   *
   * @return the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} that will yield the
   *         {@link ProcessorInterceptor}s to be applied on each component of a flow.
   */
  List<ProcessorInterceptorFactory> getInterceptorFactories();
}
