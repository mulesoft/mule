/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.metadata.MetadataEnrichableModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.map.HashedMap;

/**
 * {@link ConfigurationProvider} which provides always the same {@link #configuration}.
 *
 * @param <T> the generic type of the instances provided
 * @since 3.7.0
 */
public final class StaticConfigurationProvider<T> extends LifecycleAwareConfigurationProvider<T> {

  private final ConfigurationInstance<T> configuration;

  @Inject
  private MuleMetadataManager metadataManager;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  public StaticConfigurationProvider(String name, RuntimeConfigurationModel model, ConfigurationInstance<T> configuration) {
    super(name, model);
    this.configuration = configuration;
    registerConfiguration(configuration);
  }

  /**
   * Returns {@link #configuration}.
   *
   * @param muleEvent the current {@link MuleEvent}
   * @return {@link #configuration}
   */
  @Override
  public ConfigurationInstance<T> get(Object muleEvent) {
    return configuration;
  }

  @Override
  public MetadataResult<Map<String, Set<MetadataKey>>> getMetadataKeys() throws MetadataResolvingException {

    Map<String, Set<MetadataKey>> keys = new HashedMap();
    MetadataContext metadataContext = getMetadataContext();
    try {
      addComponentKeys(configuration.getModel().getOperationModels(), metadataContext, keys);
      addComponentKeys(configuration.getModel().getSourceModels(), metadataContext, keys);
    } catch (Exception e) {
      return MetadataResult.failure(null, format("%s: %s"), e);
    }

    return MetadataResult.success(keys);
  }

  private void addComponentKeys(List<? extends ComponentModel> components, MetadataContext metadataContext,
                                Map<String, Set<MetadataKey>> keys)
      throws MetadataResolvingException, ConnectionException {
    for (ComponentModel component : components) {
      MetadataKeysResolver keysResolver =
          ((MetadataEnrichableModel) component).getMetadataResolverFactory().getKeyResolver();
      String resolverName = keysResolver.getClass().getName();
      if (!(keysResolver instanceof NullMetadataResolver) && !keys.containsKey(resolverName)) {
        keys.put(resolverName, keysResolver.getMetadataKeys(metadataContext));
      }
    }
  }


  private MetadataContext getMetadataContext() throws MetadataResolvingException {
    return new DefaultMetadataContext((ConfigurationInstance<Object>) configuration,
                                      connectionManager,
                                      metadataManager.getMetadataCache(configuration.getName()));
  }
}
