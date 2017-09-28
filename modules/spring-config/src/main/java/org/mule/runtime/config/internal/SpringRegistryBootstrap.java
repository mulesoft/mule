/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.config.internal.factories.BootstrapObjectFactoryBean;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap;
import org.mule.runtime.core.internal.config.bootstrap.BootstrapObjectFactory;
import org.mule.runtime.core.internal.config.bootstrap.ObjectBootstrapProperty;
import org.mule.runtime.core.internal.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.runtime.core.internal.config.bootstrap.TransformerBootstrapProperty;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.internal.registry.RegistryProvider;
import org.mule.runtime.core.privileged.transformer.TransformerUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Specialization of {@link SimpleRegistryBootstrap which instead of registering the objects directly into a {@link Registry},
 * generates {@link BeanDefinition}s into a {@link BeanDefinitionRegistry} so that the Spring framework can create those objects
 * when initialising an {@link ApplicationContext}}
 *
 * @since 3.7.0
 */
public class SpringRegistryBootstrap extends AbstractRegistryBootstrap implements Initialisable {

  private OptionalObjectsController optionalObjectsController;
  private BiConsumer<String, BeanDefinition> beanDefinitionRegister;

  /**
   * @param artifactType type of artifact. Bootstrap entries may be associated to an specific type of artifact. If it's not
   *        associated to the related artifact it will be ignored.
   * @param muleContext the {@code MuleContext} of the artifact.
   * @param optionalObjectsController a controller for objects that may be optional. When an object can be optional and mule it's
   *        not able to create it, then it gets ignored.
   * @param beanDefinitionRegister a {@link BiConsumer} on which the bean definitions are registered
   */
  public SpringRegistryBootstrap(ArtifactType artifactType, MuleContext muleContext,
                                 OptionalObjectsController optionalObjectsController,
                                 BiConsumer<String, BeanDefinition> beanDefinitionRegister) {
    super(artifactType, muleContext);
    this.optionalObjectsController = optionalObjectsController;
    this.beanDefinitionRegister = beanDefinitionRegister;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    try {
      absorbAndDiscardOtherRegistries();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  protected void registerTransformers() throws MuleException {
    // no-op .. will happen on post processors
  }

  @Override
  protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                       Class<? extends Transformer> transformerClass)
      throws Exception {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(transformerClass);

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
      name = "_" + TransformerUtils.generateTransformerName(transformerClass, returnType);
    }

    builder.addPropertyValue("name", name);

    notifyIfOptional(name, bootstrapProperty.getOptional());
    doRegisterObject(name, builder);
  }

  /**
   * We want the SpringRegistry to be the only default one. This method looks for other registries and absorbs its objects into
   * the created {@code beanDefinitionRegistry}. Then, the absorbed registry is unregistered from the context
   */
  private void absorbAndDiscardOtherRegistries() {
    if (!(((MuleContextWithRegistries) muleContext).getRegistry() instanceof RegistryProvider)) {
      return;
    }

    for (Registry registry : ((RegistryProvider) ((MuleContextWithRegistries) muleContext).getRegistry()).getRegistries()) {
      if (registry instanceof SpringRegistry) {
        continue;
      }

      for (Entry<String, Object> entry : registry.lookupByType(Object.class).entrySet()) {
        registerInstance(entry.getKey(), entry.getValue());
      }

      ((MuleContextWithRegistries) muleContext).removeRegistry(registry);
    }
  }

  @Override
  protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    notifyIfOptional(bootstrapProperty.getKey(), bootstrapProperty.getOptional());

    Class<?> clazz = bootstrapProperty.getService().forName(bootstrapProperty.getClassName());
    BeanDefinitionBuilder builder;

    if (BootstrapObjectFactory.class.isAssignableFrom(clazz)) {
      final Object value = bootstrapProperty.getService().instantiateClass(bootstrapProperty.getClassName());
      setMuleContextIfNeeded(value, muleContext);
      builder = BeanDefinitionBuilder.rootBeanDefinition(BootstrapObjectFactoryBean.class);
      builder.addConstructorArgValue(value);
    } else {
      builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
    }

    doRegisterObject(bootstrapProperty.getKey(), builder);
  }

  private void notifyIfOptional(String key, boolean optional) {
    if (optional && optionalObjectsController != null) {
      optionalObjectsController.registerOptionalKey(key);
    }
  }

  private void doRegisterObject(String key, BeanDefinitionBuilder builder) {
    beanDefinitionRegister.accept(key, builder.getBeanDefinition());
  }

  private void registerInstance(String key, Object value) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConstantFactoryBean.class);
    builder.addConstructorArgValue(value);
    doRegisterObject(key, builder);
  }
}
