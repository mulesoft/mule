/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.util.StreamCloser;
import org.mule.runtime.core.privileged.registry.ObjectProcessor;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Map;

/**
 * An implementation of {@link AbstractRegistryBootstrap} to populate instances of {@link SimpleRegistry}
 *
 * @deprecated as of 3.7.0. Try to use {@link org.mule.runtime.core.config.spring.SpringRegistryBootstrap} instead.
 */
@Deprecated
public class SimpleRegistryBootstrap extends AbstractRegistryBootstrap {

  /**
   * @param supportedArtifactType type of the artifact to support. This attributes defines which types of registry bootstrap
   *        entries will be created depending on the entry applyToArtifactType parameter value.
   * @param muleContext {@code MuleContext} in which the objects will be registered
   */
  public SimpleRegistryBootstrap(ArtifactType supportedArtifactType, MuleContext muleContext) {
    super(supportedArtifactType, muleContext);
  }

  @Override
  protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                       Class<? extends Transformer> transformerClass)
      throws Exception {
    Transformer trans = ClassUtils.instantiateClass(transformerClass);
    if (!(trans instanceof DiscoverableTransformer)) {
      throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
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
    ((MuleContextWithRegistries) muleContext).getRegistry().registerTransformer(trans);
  }

  @Override
  protected void registerTransformers() throws MuleException {
    MuleRegistryHelper registry = (MuleRegistryHelper) ((MuleContextWithRegistries) muleContext).getRegistry();
    Map<String, Converter> converters = registry.lookupByType(Converter.class);
    for (Converter converter : converters.values()) {
      registry.notifyTransformerResolvers(converter, TransformerResolver.RegistryAction.ADDED);
    }
  }

  @Override
  protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    Object value = bootstrapProperty.getService().instantiateClass(bootstrapProperty.getClassName());
    Class<?> meta = Object.class;

    if (value instanceof ObjectProcessor) {
      meta = ObjectProcessor.class;
    } else if (value instanceof StreamCloser) {
      meta = StreamCloser.class;
    } else if (value instanceof BootstrapObjectFactory) {
      setMuleContextIfNeeded(value, muleContext);
      value = ((BootstrapObjectFactory) value).create();
    }
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(bootstrapProperty.getKey(), value, meta);
  }
}
