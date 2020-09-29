/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.metadata.cache.DefaultMetadataCache;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MetadataExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExecutor.class);

  protected ConnectionManager connectionManager;
  protected ReflectionCache reflectionCache;
  protected ArtifactHelper artifactHelper;

  public MetadataExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    this.connectionManager = connectionManager;
    this.reflectionCache = reflectionCache;
    this.artifactHelper = artifactHelper;
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

  protected MetadataContext createMetadataContext(Optional<ConfigurationInstance> configurationInstance,
                                                  ClassLoader extensionClassLoader) {
    return new DefaultMetadataContext(() -> configurationInstance,
                                      connectionManager,
                                      new DefaultMetadataCache(),
                                      new JavaTypeLoader(extensionClassLoader));
  }

  protected static <T> T withMetadataContext(MetadataContext metadataContext, Callable<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      metadataContext.dispose();
    }
  }

}
