/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEMORY_MANAGEMENT_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.context.SpringMuleContextServiceConfigurator;
import org.mule.runtime.config.internal.lazy.NoOpConnectivityTesterFactory;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.registry.MuleRegistry;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Specialization of SpringMuleContextServiceConfigurator that declares beans override for lazyInit.
 *
 * @since 4.5
 */
public class LazySpringMuleContextServiceConfigurator extends SpringMuleContextServiceConfigurator {

  private final LazyComponentInitializerAdapter lazyComponentInitializer;

  public LazySpringMuleContextServiceConfigurator(LazyComponentInitializerAdapter lazyComponentInitializer,
                                                  Map<String, String> artifactProperties,
                                                  MuleContextWithRegistry muleContext,
                                                  MuleFunctionsBindingContextProvider coreFunctionsProvider,
                                                  ConfigurationProperties configurationProperties,
                                                  ArtifactType artifactType,
                                                  ArtifactAst artifactAst,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator,
                                                  ResourceLocator resourceLocator,
                                                  MemoryManagementService memoryManagementService) {
    super(muleContext, coreFunctionsProvider, configurationProperties, artifactProperties,
          artifactType,
          artifactAst,
          beanDefinitionRegistry,
          serviceLocator, resourceLocator, memoryManagementService);
    this.lazyComponentInitializer = lazyComponentInitializer;
  }

  @Override
  protected void createArtifactServices() {
    super.createArtifactServices();

    registerBeanDefinition(OBJECT_CONNECTIVITY_TESTER_FACTORY, getBeanDefinition(NoOpConnectivityTesterFactory.class));
    registerConstantBeanDefinition(MULE_MEMORY_MANAGEMENT_SERVICE, getMemoryManagementService(), true);
    registerConstantBeanDefinition(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, lazyComponentInitializer, true);
  }

  protected MuleRegistry getRegistry() {
    return getMuleContext().getRegistry();
  }

}
