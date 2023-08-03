/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.extension.internal.util.ExtensionConnectivityUtils.isConnectionProvisioningRequired;
import static org.mule.runtime.module.extension.internal.util.ReconnectionUtils.addCursorResetInterceptorsIfRequired;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * Utilities related with the creation of interception chains.
 *
 * @since 4.5.0
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
   * @param initialSpanInfo    a {@link InitialSpanInfo} for the span representing the get-connection.
   * @return a new {@link InterceptorChain}
   * @since 4.5.0
   */
  public static InterceptorChain createConnectionInterceptorsChain(ExtensionModel extensionModel,
                                                                   ComponentModel componentModel,
                                                                   ExtensionConnectionSupplier connectionSupplier,
                                                                   ReflectionCache reflectionCache,
                                                                   ComponentTracer<CoreEvent> connectionComponentTracer) {
    InterceptorChain.Builder chainBuilder = InterceptorChain.builder();

    if (requiresConnectionInterceptors(extensionModel, componentModel)) {
      addConnectionInterceptors(chainBuilder, connectionSupplier, connectionComponentTracer);
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
                                                ExtensionConnectionSupplier connectionSupplier,
                                                ComponentTracer<CoreEvent> connectionComponentTracer) {
    chainBuilder.addInterceptor(new ConnectionInterceptor(connectionSupplier, connectionComponentTracer));
  }
}
