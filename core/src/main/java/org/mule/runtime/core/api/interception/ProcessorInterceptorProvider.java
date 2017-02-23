/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.interception;

import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;

import java.util.List;

// TODO MULE-11521 Define if this will remain here
public interface ProcessorInterceptorProvider {

  /**
   * Determines the order in which the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} products will be applied
   * to the applicable components.
   * <p>
   * For each {@link ProcessorInterceptorFactory factory}, its fully qualified class name will be obtained and matched against the
   * passed {@code packagesOrder} to sort the factories. In the case there is more than one {@link ProcessorInterceptorFactory
   * factory} with a package name prefix, the order in which they were {@link #addInterceptorFactory(ProcessorInterceptorFactory)
   * added} will be kept.
   * <p>
   * Assuming this is called with parameters {@code ("org.package", "com.plugin")}, and the following
   * {@link ProcessorInterceptorFactory factories} have been added through
   * {@link #addInterceptorFactory(ProcessorInterceptorFactory)} (in this order):
   * <ol>
   * <li>{@code com.plugin.SomeInterceptor}</li>
   * <li>{@code org.mule.MuleInterceptor}</li>
   * <li>{@code org.package.logging.LoggerInterceptor}</li>
   * <li>{@code com.plugin.SomeOtherInterceptor}</li>
   * <li>{@code org.mule.OtherMuleInterceptor}</li>
   * </ol>
   * Those {@link ProcessorInterceptorFactory factories} will be sorted, when obtained through {@link #getInterceptorFactories()}
   * like this:
   * <ol>
   * <li>{@code org.package.logging.LoggerInterceptor}</li>
   * <li>{@code com.plugin.SomeInterceptor}</li>
   * <li>{@code com.plugin.SomeOtherInterceptor}</li>
   * <li>{@code org.mule.MuleInterceptor}</li>
   * <li>{@code org.mule.OtherMuleInterceptor}</li>
   * </ol>
   * 
   * @param packagesOrder the wanted order for the interceptors.
   */
  void setInterceptorsOrder(String... packagesOrder);

  /**
   * Adds an {@link ProcessorInterceptorFactory} to be applied to the components of a flow.
   * <p>
   * By default, the created {@link ProcessorInterceptor}s will be applied in the same order as they were added by calling this
   * method. To change this default ordering, {@link #setInterceptorsOrder(String...)} must be called with the desired order.
   * 
   * @param interceptorFactory the factory of {@link ProcessorInterceptor}s to add.
   */
  void addInterceptorFactory(ProcessorInterceptorFactory interceptorFactory);

  /**
   * Provides the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} that were registered by calling
   * {@link #addInterceptorFactory(ProcessorInterceptorFactory)}, in the order defined by {@link #setInterceptorsOrder(String...)}
   * if defined.
   * 
   * @return the {@link ProcessorInterceptorFactory ProcessorInterceptorFactories} that will yield the
   *         {@link ProcessorInterceptor}s to be applied on each component of a flow.
   */
  List<ProcessorInterceptorFactory> getInterceptorFactories();
}
