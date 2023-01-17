/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.extension.internal.util.ExtensionConnectivityUtils.isConnectionProvisioningRequired;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.addCursorResetInterceptorsIfRequired;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;

/**
 * Utilities related with the creation of interception chains.
 *
 * @since 4.6.0
 */
public class InterceptorChainUtils {

  private InterceptorChainUtils() {}

  /**
   * Creates an {@link InterceptorChain} with the interceptors necessary for connectable components.
   *
   * @param extensionModel     the {@link ExtensionModel}
   * @param componentModel     the {@link ComponentModel}
   * @param connectionSupplier the connection supplier
   * @param reflectionCache    a {@link ReflectionCache}
   * @return a new {@link InterceptorChain}
   * @since 4.6.0
   */
  public static InterceptorChain createConnectionInterceptorsChain(ExtensionModel extensionModel,
                                                                   ComponentModel componentModel,
                                                                   ExtensionConnectionSupplier connectionSupplier,
                                                                   ReflectionCache reflectionCache) {
    InterceptorChain.Builder chainBuilder = InterceptorChain.builder();

    if (requiresConnectionInterceptors(extensionModel, componentModel)) {
      addConnectionInterceptors(chainBuilder, connectionSupplier);
    }

    addCursorResetInterceptorsIfRequired(chainBuilder, extensionModel, componentModel, reflectionCache);

    return chainBuilder.build();
  }

  private static boolean requiresConnectionInterceptors(ExtensionModel extensionModel, ComponentModel componentModel) {
    // Only connectable components that require a connection to be provided beforehand should add the connection interceptors
    if (componentModel instanceof ConnectableComponentModel) {
      return ((ConnectableComponentModel) componentModel).requiresConnection()
          && isConnectionProvisioningRequired(extensionModel, (ConnectableComponentModel) componentModel);
    }

    return false;
  }

  private static void addConnectionInterceptors(InterceptorChain.Builder chainBuilder,
                                                ExtensionConnectionSupplier connectionSupplier) {
    chainBuilder.addInterceptor(new ConnectionInterceptor(connectionSupplier));
  }
}
