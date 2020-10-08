/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Optional;

public class MetadataKeysExecutor extends MetadataExecutor {

  public MetadataKeysExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache,
                              ArtifactHelper artifactHelper) {
    super(connectionManager, reflectionCache, artifactHelper);
  }

  public MetadataResult<MetadataKeysContainer> resolveMetadataKeys(ComponentModel componentModel,
                                                                   ComponentElementDeclaration componentElementDeclaration) {
    try {
      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(componentModel, componentElementDeclaration);

      MetadataKey metadataKey = new MetadataKeyDeclarationResolver(componentModel, componentElementDeclaration).resolveKey();

      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(componentElementDeclaration));

      MetadataMediator<ComponentModel> metadataMediator = new MetadataMediator<>(componentModel);

      MetadataContext metadataContext =
          createMetadataContext(optionalConfigurationInstance, extensionClassLoader);

      return withContextClassLoader(extensionClassLoader,
                                    () -> withMetadataContext(metadataContext, () -> metadataMediator
                                        .getMetadataKeys(metadataContext, metadataKey, reflectionCache)));
    } catch (ExpressionNotSupportedException e) {
      return failure(MetadataFailure.Builder.newFailure(e).withFailureCode(INVALID_METADATA_KEY).onKeys());
    } catch (MetadataResolvingException e) {
      return failure(MetadataFailure.Builder.newFailure(e).withFailureCode(e.getFailure()).onKeys());
    } catch (Exception e) {
      return failure(MetadataFailure.Builder.newFailure(e).onKeys());
    }
  }

  @Override
  protected boolean resolverRequiresConfiguration(Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty,
                                                  ComponentModel componentModel) {
    //TODO MULE-15638 it is not correct the information provided by the TypeResolversInformationModelProperty model property
    //return typeResolversInformationModelProperty.map(mp ->  mp.getKeysResolver()
    //                .map(resolverInformation -> resolverInformation.isRequiresConfiguration()).orElse(false))
    //        .orElse(false);
    return artifactHelper.hasParameterOfType(componentModel, CONFIG);
  }

}
