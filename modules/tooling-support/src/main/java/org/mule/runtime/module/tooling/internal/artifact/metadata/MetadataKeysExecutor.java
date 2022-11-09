/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static com.google.common.base.Throwables.propagateIfPossible;
import static java.lang.String.format;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.sampledata.SampleDataFailure.Builder.newFailure;
import static org.mule.runtime.api.sampledata.SampleDataResult.resultFrom;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.MetadataCacheManager;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.AbstractParameterResolverExecutor;
import org.mule.runtime.module.tooling.internal.artifact.ExecutorExceptionWrapper;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataKeysExecutor extends MetadataExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataKeysExecutor.class);

  private static final FailureCode INVALID_PARAMETER_VALUE =
      new FailureCode(AbstractParameterResolverExecutor.INVALID_PARAMETER_VALUE);

  public MetadataKeysExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache,
                              ExpressionManager expressionManager, ArtifactHelper artifactHelper,
                              MetadataCacheIdGenerator<ElementDeclaration> metadataCacheIdGenerator,
                              MetadataCacheManager metadataCacheManager) {
    super(connectionManager, reflectionCache, expressionManager, artifactHelper, metadataCacheIdGenerator,
          metadataCacheManager);
  }

  public MetadataResult<MetadataKeysContainer> resolveMetadataKeys(ComponentModel componentModel,
                                                                   ComponentElementDeclaration componentElementDeclaration) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resolve metadata keys STARTED for component: {}", componentModel.getName());
      }
      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(componentModel, componentElementDeclaration);

      MetadataKey metadataKey =
          new MetadataKeyDeclarationResolver(componentModel, componentElementDeclaration, expressionManager).resolvePartialKey();

      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(componentElementDeclaration));

      MetadataMediator<ComponentModel> metadataMediator = new MetadataMediator<>(componentModel);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invoking connector's metadata key resolver for component: {}", componentModel.getName());
      }
      return withContextClassLoader(extensionClassLoader,
                                    () -> runWithMetadataContext(componentElementDeclaration, optionalConfigurationInstance,
                                                                 extensionClassLoader,
                                                                 (metadataContext) -> metadataMediator
                                                                     .getMetadataKeys(metadataContext, metadataKey,
                                                                                      reflectionCache)),
                                    MetadataResolvingException.class, e -> {
                                      throw new ExecutorExceptionWrapper(e);
                                    });
    } catch (MetadataResolvingException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Resolve metadata keys has FAILED with code: %s for component: %s", e.getFailure(),
                           componentModel.getName()),
                    e);
      }
      return failure(MetadataFailure.Builder.newFailure(e).withFailureCode(e.getFailure()).onKeys());
    } catch (ExpressionNotSupportedException e) {
      return failure(MetadataFailure.Builder.newFailure(e).withFailureCode(INVALID_PARAMETER_VALUE).onKeys());
    } catch (ExecutorExceptionWrapper e) {
      Throwable cause = e.getCause();
      if (cause instanceof MetadataResolvingException) {
        MetadataResolvingException metadataResolvingException = (MetadataResolvingException) cause;
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(format("Resolve metadata keys has FAILED with code: %s for component: %s",
                             metadataResolvingException.getFailure(), componentModel.getName()),
                      cause);
        }
        return failure(MetadataFailure.Builder.newFailure(e).withFailureCode(metadataResolvingException.getFailure())
            .onKeys());
      }
      propagateIfPossible(cause, MuleRuntimeException.class);
      throw new MuleRuntimeException(cause);
    } catch (Exception e) {
      propagateIfPossible(e, MuleRuntimeException.class);
      throw new MuleRuntimeException(e);
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Resolve metadata keys FINISHED for component: {}", componentModel.getName());
      }
    }
  }

  @Override
  protected boolean resolverRequiresConfiguration(Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty,
                                                  ComponentModel componentModel) {
    // TODO MULE-15638 it is not correct the information provided by the TypeResolversInformationModelProperty model property
    // return typeResolversInformationModelProperty.map(mp -> mp.getKeysResolver()
    // .map(resolverInformation -> resolverInformation.isRequiresConfiguration()).orElse(false))
    // .orElse(false);
    return artifactHelper.hasParameterOfType(componentModel, CONFIG);
  }

}
