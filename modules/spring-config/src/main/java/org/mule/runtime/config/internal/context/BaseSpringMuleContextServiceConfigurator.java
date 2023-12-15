/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONFIGURATION_PROPERTIES;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_POOLS_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMERS_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSFORMER_RESOLVER;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.BINDING_PROVIDER_PREDICATE;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.TRANSFORMER_PREDICATE;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.SchedulerContainerPoolsConfig;
import org.mule.runtime.config.internal.el.DataWeaveExtendedExpressionLanguageAdaptorFactoryBean;
import org.mule.runtime.config.internal.el.DefaultExpressionManagerFactoryBean;
import org.mule.runtime.config.internal.factories.SchedulerBaseConfigFactory;
import org.mule.runtime.config.internal.lazy.LazyDataWeaveExtendedExpressionLanguageAdaptorFactoryBean;
import org.mule.runtime.config.internal.registry.OptionalObjectsController;
import org.mule.runtime.config.internal.registry.SpringRegistryBootstrap;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.registry.TypeBasedTransformerResolver;
import org.mule.runtime.core.internal.transformer.DefaultTransformersRegistry;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * This class configures the basic services available in a {@code MuleContext} that are independent of the artifact config.
 * <p>
 * There's a predefined set of services plus a configurable set of services provided by
 * {@code MuleContext#getCustomizationService}.
 * <p>
 * This class takes cares of registering bean definitions for each of the provided services so dependency injection can be
 * properly done through the use of {@link Inject}.
 *
 * @since 4.5
 */
public class BaseSpringMuleContextServiceConfigurator extends AbstractSpringMuleContextServiceConfigurator {

  // This is needed just for some unit test scenarios
  // TODO MULE-20028 remove this
  @Deprecated
  public static final String DISABLE_TRANSFORMERS_SUPPORT =
      BaseSpringMuleContextServiceConfigurator.class.getName() + ".disableTransformersSupport";

  private final MuleContext muleContext;
  private final ArtifactType artifactType;
  private final OptionalObjectsController optionalObjectsController;
  private final ConfigurationProperties configurationProperties;
  private final boolean enableLazyInit;
  private org.mule.runtime.core.internal.registry.Registry originalRegistry;

  public BaseSpringMuleContextServiceConfigurator(MuleContext muleContext,
                                                  ConfigurationProperties configurationProperties,
                                                  ArtifactType artifactType,
                                                  OptionalObjectsController optionalObjectsController,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator,
                                                  org.mule.runtime.core.internal.registry.Registry originalRegistry,
                                                  boolean enableLazyInit) {
    super((InternalCustomizationService) muleContext.getCustomizationService(), beanDefinitionRegistry, serviceLocator);
    this.muleContext = muleContext;
    this.configurationProperties = configurationProperties;
    this.artifactType = artifactType;
    this.optionalObjectsController = optionalObjectsController;
    this.originalRegistry = originalRegistry;
    this.enableLazyInit = enableLazyInit;
  }

  void createArtifactServices() {
    registerConstantBeanDefinition(FEATURE_FLAGGING_SERVICE_KEY, originalRegistry.lookupObject(FEATURE_FLAGGING_SERVICE_KEY));

    registerConstantBeanDefinition(ConfigurationComponentLocator.REGISTRY_KEY, new BaseConfigurationComponentLocator());

    // Instances of the repository and locator need to be injected into another objects before actually determining the possible
    // values. This contributing layer is needed to ensure the correct functioning of the DI mechanism while allowing actual
    // values to be provided at a later time.
    final ContributedErrorTypeRepository contributedErrorTypeRepository = new ContributedErrorTypeRepository();
    registerConstantBeanDefinition(ErrorTypeRepository.class.getName(), contributedErrorTypeRepository);
    final ContributedErrorTypeLocator contributedErrorTypeLocator = new ContributedErrorTypeLocator();
    contributedErrorTypeLocator.setDelegate(createDefaultErrorTypeLocator(contributedErrorTypeRepository));
    registerConstantBeanDefinition(ErrorTypeLocator.class.getName(), contributedErrorTypeLocator);

    registerConstantBeanDefinition(OBJECT_CONFIGURATION_PROPERTIES, configurationProperties);

    if (!getBoolean(DISABLE_TRANSFORMERS_SUPPORT)) {
      registerBeanDefinition(OBJECT_TRANSFORMER_RESOLVER, getBeanDefinition(TypeBasedTransformerResolver.class));
      registerBeanDefinition(OBJECT_TRANSFORMERS_REGISTRY, getBeanDefinition(DefaultTransformersRegistry.class));
    }

    registerLazyInitialisationAwareBeans();

    registerBeanDefinition(OBJECT_EXPRESSION_LANGUAGE, getBeanDefinition(MVELExpressionLanguage.class));
    registerBeanDefinition(OBJECT_EXPRESSION_MANAGER, getBeanDefinition(DefaultExpressionManagerFactoryBean.class));

    registerBeanDefinition(OBJECT_SCHEDULER_POOLS_CONFIG,
                           getConstantObjectBeanDefinition(SchedulerContainerPoolsConfig.getInstance()));
    registerBeanDefinition(OBJECT_SCHEDULER_BASE_CONFIG, getBeanDefinition(SchedulerBaseConfigFactory.class));

    registerConstantBeanDefinition(OBJECT_REGISTRY, getServiceLocator());

    createRuntimeServices();
    createBootstrapBeanDefinitions();
    absorbOriginalRegistry();
  }

  private void registerLazyInitialisationAwareBeans() {
    if (enableLazyInit) {
      registerBeanDefinition(OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER,
                             getBeanDefinition(LazyDataWeaveExtendedExpressionLanguageAdaptorFactoryBean.class));
    } else {
      registerBeanDefinition(OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER,
                             getBeanDefinition(DataWeaveExtendedExpressionLanguageAdaptorFactoryBean.class));
    }
  }

  protected void createBootstrapBeanDefinitions() {
    try {
      SpringRegistryBootstrap springRegistryBootstrap =
          new SpringRegistryBootstrap(artifactType, muleContext, optionalObjectsController, this::registerBeanDefinition,
                                      BINDING_PROVIDER_PREDICATE
                                          .or(TRANSFORMER_PREDICATE)
                                          .or(propertyKey -> propertyKey.endsWith(COMPATIBILITY_PLUGIN_INSTALLED)));
      springRegistryBootstrap.initialise();
    } catch (InitialisationException e) {
      throw new RuntimeException(e);
    }
  }

  private void createRuntimeServices() {
    final Map<String, CustomService> customServices = getCustomizationService().getCustomServices();
    for (String serviceName : customServices.keySet()) {

      if (containsBeanDefinition(serviceName)) {
        throw new IllegalStateException("There is already a bean definition registered with key: " + serviceName);
      }

      final CustomService customService = customServices.get(serviceName);
      // TODO MULE-19927 get these form a more specific place and avoid this filter
      if (isServiceRuntimeProvided(customService)) {
        final BeanDefinition beanDefinition = getCustomServiceBeanDefinition(customService, serviceName);

        registerBeanDefinition(serviceName, beanDefinition);
      }
    }
  }

  private void absorbOriginalRegistry() {
    if (originalRegistry == null) {
      return;
    }

    originalRegistry.lookupByType(Object.class)
        .forEach((key, value) -> registerConstantBeanDefinition(key, value));
    originalRegistry = null;
  }

}
