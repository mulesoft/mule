/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformerNotImplementDiscoverable;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.builders.RegistryBootstrap;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.privileged.registry.RegistrationException;

/**
 * Basic implementation of {@link RegistryBootstrap}
 *
 * @since 4.5.0
 */
public class SimpleRegistryBootstrap extends AbstractRegistryBootstrap {

  private final Registrer registrer;

  /**
   * @param supportedArtifactType      type of the artifact to support. This attributes defines which types of registry bootstrap
   *                                   entries will be created depending on the entry applyToArtifactType parameter value.
   * @param bootstrapServiceDiscoverer {@link BootstrapServiceDiscoverer} used to bootstrap a {@link MuleContext}
   * @param registrer                  adds an object to the registry, with any applicable rules (i.e.: service overrides)
   */
  public SimpleRegistryBootstrap(ArtifactType supportedArtifactType, BootstrapServiceDiscoverer bootstrapServiceDiscoverer,
                                 Registrer registrer) {
    super(supportedArtifactType, bootstrapServiceDiscoverer, k -> true);
    this.registrer = registrer;
  }

  @Override
  protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                       Class<? extends Transformer> transformerClass)
      throws Exception {
    Transformer trans = ClassUtils.instantiateClass(transformerClass);
    if (!(trans instanceof DiscoverableTransformer)) {
      throw new RegistrationException(transformerNotImplementDiscoverable(trans));
    }
    if (returnClass != null) {
      DataTypeParamsBuilder builder = DataType.builder().type(returnClass);
      if (isNotEmpty(bootstrapProperty.getMimeType())) {
        builder = builder.mediaType(bootstrapProperty.getMimeType());
      }
      trans.setReturnDataType(builder.build());
    }
    if (bootstrapProperty.getName() != null) {
      trans.setName(bootstrapProperty.getName());
    } else {
      // Prefixes the generated default name to ensure there is less chance of conflict if the user registers
      // the transformer with the same name
      trans.setName("_" + trans.getName());
    }
    registrer.register(trans.getName(), trans);
  }

  @Override
  protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    registrer.register(bootstrapProperty.getKey(),
                       bootstrapProperty.getService().instantiateClass(bootstrapProperty.getClassName()));
  }
}
