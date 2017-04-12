/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.extension.api.metadata.NullMetadataResolver.NULL_CATEGORY_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMetadataResolverFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.List;
import java.util.Optional;

/**
 * Adds the capability to expose all the {@link MetadataKey}s associated with the {@link StaticConfigurationProvider}'s
 * components.
 *
 * @since 4.0
 */
public final class ConfigurationProviderMetadataAdapter extends StaticConfigurationProvider
    implements MetadataKeyProvider {

  private final MuleMetadataService metadataService;
  protected final ConnectionManager connectionManager;

  public ConfigurationProviderMetadataAdapter(String name,
                                              ExtensionModel extensionModel,
                                              ConfigurationModel configurationModel,
                                              ConfigurationInstance configuration,
                                              MuleContext muleContext) {
    super(name, extensionModel, configurationModel, configuration, muleContext);
    this.metadataService = muleContext.getRegistry().get(OBJECT_METADATA_SERVICE);
    this.connectionManager = muleContext.getRegistry().get(OBJECT_CONNECTION_MANAGER);
  }

  public MetadataResult<MetadataKeysContainer> getMetadataKeys() throws MetadataResolvingException {

    MetadataKeysContainerBuilder keysBuilder = MetadataKeysContainerBuilder.getInstance();
    try {
      MetadataContext metadataContext = getMetadataContext();
      addComponentKeys(getConfigurationModel().getOperationModels(), metadataContext, keysBuilder);
      addComponentKeys(getConfigurationModel().getSourceModels(), metadataContext, keysBuilder);
      metadataContext.dispose();
    } catch (Exception e) {
      return failure(newFailure(e).onKeys());
    }
    return success(keysBuilder.build());
  }

  private void addComponentKeys(List<? extends ComponentModel> components, MetadataContext metadataContext,
                                MetadataKeysContainerBuilder keysBuilder)
      throws MetadataResolvingException, ConnectionException {
    for (ComponentModel component : components) {
      TypeKeysResolver keysResolver = getMetadataResolverFactory(component).getKeyResolver();

      String categoryName = keysResolver.getCategoryName();
      if (!NULL_CATEGORY_NAME.equals(categoryName) && !keysBuilder.containsCategory(categoryName)) {
        keysBuilder.add(categoryName, keysResolver.getKeys(metadataContext));
      }
    }
  }

  private MetadataContext getMetadataContext() throws MetadataResolvingException, ConnectionException {
    Event fakeEvent = getInitialiserEvent(muleContext);
    return new DefaultMetadataContext(Optional.of(get(fakeEvent)),
                                      connectionManager,
                                      metadataService.getMetadataCache(getName()),
                                      ExtensionsTypeLoaderFactory.getDefault()
                                          .createTypeLoader(getClassLoader(getExtensionModel())));
  }
}
