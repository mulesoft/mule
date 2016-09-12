/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils.buildImplicitResolverSet;
import static org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils.getFirstImplicit;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getAllConnectionProviders;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Default implementation of {@link ImplicitConnectionProviderFactory}
 *
 * @since 4.0
 */
public final class DefaultImplicitConnectionProviderFactory implements ImplicitConnectionProviderFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public <Connector> ConnectionProvider<Connector> createImplicitConnectionProvider(String configName,
                                                                                    RuntimeConfigurationModel configurationModel,
                                                                                    Event event, MuleContext muleContext) {
    RuntimeConnectionProviderModel implicitModel =
        (RuntimeConnectionProviderModel) getFirstImplicit(getAllConnectionProviders(configurationModel));

    if (implicitModel == null) {
      throw new IllegalStateException(format(
                                             "Configuration '%s' of extension '%s' does not define a connection provider and none can be created automatically. Please define one.",
                                             configName, configurationModel.getName()));
    }

    final ResolverSet resolverSet = buildImplicitResolverSet(implicitModel, muleContext);
    ConnectionProviderObjectBuilder builder =
        new ConnectionProviderObjectBuilder(implicitModel, resolverSet, muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER));
    builder.setOwnerConfigName(configName);

    try {
      return builder.build(event);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }
}
