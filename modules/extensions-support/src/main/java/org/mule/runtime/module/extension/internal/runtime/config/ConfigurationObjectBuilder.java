/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.ParameterGroupAwareObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Implementation of {@link ObjectBuilder} to create instances that match a given {@link RuntimeConfigurationModel}.
 * <p>
 * The object instances are created through the {@link RuntimeConfigurationModel#getConfigurationFactory()#instantiateObject()}
 * method. A {@link ResolverSet} is also used to automatically set this builders properties. The name of the properties in the
 * {@link ResolverSet} must match the name of an actual property in the prototype class
 *
 * @since 3.7.0
 */
public final class ConfigurationObjectBuilder<T> extends ParameterGroupAwareObjectBuilder<T> {

  private final RuntimeConfigurationModel configurationModel;

  public ConfigurationObjectBuilder(RuntimeConfigurationModel configurationModel, ResolverSet resolverSet) {
    super(configurationModel.getConfigurationFactory().getObjectType(), configurationModel, resolverSet);
    this.configurationModel = configurationModel;
  }

  /**
   * Creates a new instance by calling {@link RuntimeConfigurationModel#getConfigurationFactory()#instantiateObject()}
   * {@inheritDoc}
   */
  @Override
  protected T instantiateObject() {
    return (T) configurationModel.getConfigurationFactory().newInstance();
  }
}
