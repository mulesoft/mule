/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.empty;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;

import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.internal.metadata.cache.DefaultMetadataCache;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheManager;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetadataExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExecutor.class);

  protected ConnectionManager connectionManager;
  protected ReflectionCache reflectionCache;
  protected ExpressionManager expressionManager;
  protected ArtifactHelper artifactHelper;
  private MetadataCacheIdGenerator<ElementDeclaration> metadataCacheIdGenerator;
  private MetadataCacheManager metadataCacheManager;

  public MetadataExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache,
                          ExpressionManager expressionManager, ArtifactHelper artifactHelper,
                          MetadataCacheIdGenerator<ElementDeclaration> metadataCacheIdGenerator,
                          MetadataCacheManager metadataCacheManager) {
    this.connectionManager = connectionManager;
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.artifactHelper = artifactHelper;
    this.metadataCacheIdGenerator = metadataCacheIdGenerator;
    this.metadataCacheManager = metadataCacheManager;
  }

  protected Optional<ConfigurationInstance> getConfigurationInstance(ComponentModel componentModel,
                                                                     ComponentElementDeclaration componentElementDeclaration)
      throws MetadataResolvingException {
    Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty =
        componentModel.getModelProperty(TypeResolversInformationModelProperty.class);
    String configRef = componentElementDeclaration.getConfigRef();

    Optional<ConfigurationInstance> optionalConfigurationInstance = empty();

    if (resolverRequiresConfiguration(typeResolversInformationModelProperty, componentModel)) {
      if (configRef != null) {
        optionalConfigurationInstance = artifactHelper.getConfigurationInstance(configRef);
        if (!optionalConfigurationInstance.isPresent()) {
          throw new MetadataResolvingException(
                                               format("The resolver requires a configuration but the one referenced by the component declaration with name: '%s' is not present",
                                                      configRef),
                                               COMPONENT_NOT_FOUND);
        }
      }
    }
    return optionalConfigurationInstance;
  }

  protected abstract boolean resolverRequiresConfiguration(Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty,
                                                           ComponentModel componentModel);

  private MetadataContext createMetadataContext(Optional<ConfigurationInstance> configurationInstance,
                                                ClassLoader extensionClassLoader,
                                                MetadataCache metadataCache) {
    return new DefaultMetadataContext(() -> configurationInstance,
                                      connectionManager,
                                      metadataCache,
                                      new JavaTypeLoader(extensionClassLoader)) {

      @Override
      public <C> Optional<C> getConnection() throws ConnectionException {
        long startTime = currentTimeMillis();
        try {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retrieving connection");
          }
          return super.getConnection();
        } finally {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Connection retrieved in {}ms", (currentTimeMillis() - startTime));
          }
        }
      }
    };
  }

  protected <T> MetadataResult<T> runWithMetadataContext(ComponentElementDeclaration componentElementDeclaration,
                                                         Optional<ConfigurationInstance> configurationInstance,
                                                         ClassLoader extensionClassLoader,

                                                         Function<MetadataContext, MetadataResult<T>> contextConsumer) {
    MetadataContext metadataContext = null;
    try {
      MetadataCacheId cacheId = getMetadataCacheId(componentElementDeclaration);
      MetadataCache metadataCache = getOrCreateCache(cacheId.getValue());

      MetadataResult<T> result =
          contextConsumer.apply(createMetadataContext(configurationInstance, extensionClassLoader, metadataCache));
      if (result.isSuccess()) {
        updateCache(cacheId.getValue(), metadataCache);
      }
      return result;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      if (metadataContext != null) {
        metadataContext.dispose();
      }
    }
  }

  private void updateCache(String id, MetadataCache metadataCache) {
    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Updating MetadataCache entry for id: {}", id);
      }
      metadataCacheManager.updateCache(id, metadataCache);
    } catch (Exception e) {
      LOGGER
          .warn("Couldn't update the MetadataCache due to an internal error, data would be lost and resolvers couldn't share resources between resolutions.",
                e);
    }
  }

  private MetadataCache getOrCreateCache(String id) {
    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Creating new MetadataCache with id: {}", id);
      }
      return metadataCacheManager.getOrCreateCache(id);
    } catch (Exception e) {
      LOGGER
          .warn("Couldn't create a MetadataCache due to an internal error, resolvers won't be able to store resources between resolutions. Using an in memory cache.",
                e);
      return new DefaultMetadataCache();
    }
  }

  private MetadataCacheId getMetadataCacheId(ComponentElementDeclaration componentElementDeclaration) {
    return metadataCacheIdGenerator.getIdForGlobalMetadata(componentElementDeclaration)
        .map(id -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ID generated for MetadataCache with value: {}", id.getParts().toString());
          }
          return id;
        })
        .orElseThrow(() -> new IllegalStateException(
                                                     format("Missing information to obtain the MetadataCache for the component '%s:%s'",
                                                            componentElementDeclaration.getDeclaringExtension(),
                                                            componentElementDeclaration.getName())));
  }

  public void disposeMetadataCache(ComponentElementDeclaration componentElementDeclaration) {
    try {
      String id = getMetadataCacheId(componentElementDeclaration).getValue();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Disposing MetadataCache entry for id: {}", id);
      }
      metadataCacheManager.dispose(id);
    } catch (Exception e) {
      LOGGER.warn("Couldn't dispose MetadataCache due to an internal error.", e);
    }
  }
}
