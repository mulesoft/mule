/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.core.internal.transformer.TransformerUtils.generateTransformerName;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap;
import org.mule.runtime.core.internal.config.bootstrap.ObjectBootstrapProperty;
import org.mule.runtime.core.internal.config.bootstrap.TransformerBootstrapProperty;
import org.mule.runtime.core.internal.registry.Registry;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

/**
 * Specialization of {@link AbstractRegistryBootstrap} which instead of registering the objects directly into a {@link Registry},
 * generates {@link BeanDefinition}s into a {@link BeanDefinitionRegistry} so that the Spring framework can create those objects
 * when initialising an {@link ApplicationContext}}
 *
 * @since 3.7.0
 */
public class SpringRegistryBootstrap extends AbstractRegistryBootstrap implements Initialisable {

  private final BiConsumer<String, BeanDefinition> beanDefinitionRegister;

  /**
   * @param artifactType               type of artifact. Bootstrap entries may be associated to an specific type of artifact. If
   *                                   it's not associated to the related artifact it will be ignored.
   * @param bootstrapServiceDiscoverer {@link BootstrapServiceDiscoverer} used to bootstrap a {@link MuleContext}
   * @param beanDefinitionRegister     a {@link BiConsumer} on which the bean definitions are registered
   */
  public SpringRegistryBootstrap(ArtifactType artifactType, BootstrapServiceDiscoverer bootstrapServiceDiscoverer,
                                 BiConsumer<String, BeanDefinition> beanDefinitionRegister,
                                 Predicate<String> propertyKeyfilter) {
    super(artifactType, bootstrapServiceDiscoverer, propertyKeyfilter);
    this.beanDefinitionRegister = beanDefinitionRegister;
  }

  @Override
  protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                       Class<? extends Transformer> transformerClass)
      throws Exception {
    BeanDefinitionBuilder builder = rootBeanDefinition(transformerClass);

    DataType returnType = null;

    if (returnClass != null) {
      DataTypeParamsBuilder dataTypeBuilder = DataType.builder().type(returnClass);
      if (isNotEmpty(bootstrapProperty.getMimeType())) {
        dataTypeBuilder = dataTypeBuilder.mediaType(bootstrapProperty.getMimeType());
      }
      builder.addPropertyValue("returnDataType", dataTypeBuilder.build());
    }

    String name = bootstrapProperty.getName();
    if (name == null) {
      // Prefixes the generated default name to ensure there is less chance of conflict if the user registers
      // the transformer with the same name
      name = "_" + generateTransformerName(transformerClass, returnType);
    }

    builder.addPropertyValue("name", name);

    doRegisterObject(name, builder);
  }

  @Override
  protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    Class<?> clazz = bootstrapProperty.getService().forName(bootstrapProperty.getClassName());
    doRegisterObject(bootstrapProperty.getKey(), rootBeanDefinition(clazz));
  }

  private void doRegisterObject(String key, BeanDefinitionBuilder builder) {
    beanDefinitionRegister.accept(key, builder.getBeanDefinition());
  }

}
