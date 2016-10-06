/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver.NULL_CATEGORY_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.metadata.MetadataEnrichableModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Adds the capability to expose all the {@link MetadataKey}s associated with the {@link StaticConfigurationProvider}'s
 * components.
 *
 * @since 1.0
 */
public final class ConfigurationProviderMetadataAdapter extends StaticConfigurationProvider
    implements MetadataKeyProvider {

  @Inject
  private MuleMetadataManager metadataManager;

  @Inject
  protected ConnectionManagerAdapter connectionManager;

  public ConfigurationProviderMetadataAdapter(String name,
                                              RuntimeConfigurationModel model,
                                              ConfigurationInstance configuration) {
    super(name, model, configuration);
  }

  public MetadataResult<MetadataKeysContainer> getMetadataKeys() throws MetadataResolvingException {

    MetadataKeysContainerBuilder keysBuilder = MetadataKeysContainerBuilder.getInstance();
    MetadataContext metadataContext = getMetadataContext();
    try {
      addComponentKeys(getModel().getOperationModels(), metadataContext, keysBuilder);
      addComponentKeys(getModel().getSourceModels(), metadataContext, keysBuilder);
    } catch (Exception e) {
      return failure(null, format("%s: %s"), e);
    }

    return success(keysBuilder.build());
  }

  private void addComponentKeys(List<? extends ComponentModel> components, MetadataContext metadataContext,
                                MetadataKeysContainerBuilder keysBuilder)
      throws MetadataResolvingException, ConnectionException {
    for (ComponentModel component : components) {
      TypeKeysResolver keysResolver =
          ((MetadataEnrichableModel) component).getMetadataResolverFactory().getKeyResolver();

      String categoryName = keysResolver.getCategoryName();
      if (!NULL_CATEGORY_NAME.equals(categoryName) && !keysBuilder.containsCategory(categoryName)) {
        keysBuilder.add(categoryName, keysResolver.getKeys(metadataContext));
      }
    }
  }

  private MetadataContext getMetadataContext() throws MetadataResolvingException {
    Event fakeEvent = getInitialiserEvent(muleContext);
    return new DefaultMetadataContext(Optional.of(get(fakeEvent)),
                                      connectionManager,
                                      metadataManager.getMetadataCache(getName()),
                                      ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(
                                                                                                getClassLoader(getModel()
                                                                                                    .getExtensionModel())));
  }
}
