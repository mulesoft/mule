/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConfigurationFactory;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Implementation of {@link ObjectBuilder} to create instances that match a given {@link ConfigurationModel}.
 * <p>
 * The object instances are created through the {@link ConfigurationFactory#newInstance()}
 * method. A {@link ResolverSet} is also used to automatically set this builders properties. The name of the properties in the
 * {@link ResolverSet} must match the name of an actual property in the prototype class
 *
 * @since 3.7.0
 */
public final class ConfigurationObjectBuilder<T> extends ResolverSetBasedObjectBuilder<Pair<T, ResolverSetResult>> {

  private final ConfigurationModel configurationModel;

  public ConfigurationObjectBuilder(ConfigurationModel configurationModel, ResolverSet resolverSet) {
    super(getConfigurationFactory(configurationModel).getObjectType(), configurationModel, resolverSet);
    this.configurationModel = configurationModel;
  }

  /**
   * Creates a new instance by using the {@link ConfigurationFactory} in the {@link ConfigurationFactoryModelProperty}
   */
  @Override
  protected Pair<T, ResolverSetResult> instantiateObject() {
    return new Pair<>((T) getConfigurationFactory(configurationModel).newInstance(), null);
  }

  @Override
  public Pair<T, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    T value = instantiateObject().getFirst();
    populate(result, value);
    return new Pair<>(value, result);
  }
}
