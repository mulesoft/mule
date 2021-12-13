/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyMuleArtifactContext.SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH;
import static org.mule.runtime.config.internal.LazySampleDataService.NON_LAZY_SAMPLE_DATA_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.core.internal.metadata.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore.SHARED_PERSISTENT_OBJECT_STORE_KEY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.config.internal.lazy.LazyDataWeaveExtendedExpressionLanguageAdaptorFactoryBean;
import org.mule.runtime.config.internal.lazy.NoOpConnectivityTesterFactory;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.metadata.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.core.internal.metadata.cache.DelegateMetadataCacheManager;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.module.extension.internal.data.sample.MuleSampleDataService;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Specialization of SpringMuleContextServiceConfigurator that declares beans override for lazyInit.
 *
 * @since 4.5
 */
class LazySpringMuleContextServiceConfigurator extends SpringMuleContextServiceConfigurator {

  private static final String DEFAULT_METADATA_CACHE_MANAGER_KEY = "_defaultPersistentMetadataCacheManager";
  private static final String LAZY_MULE_OBJECT_STORE_MANAGER = "_muleLazyObjectStoreManager";

  private final LazyComponentInitializerAdapter lazyComponentInitializer;
  private final Map<String, String> artifactProperties;
  private final LockFactory runtimeLockFactory;

  public LazySpringMuleContextServiceConfigurator(LazyComponentInitializerAdapter lazyComponentInitializer,
                                                  Map<String, String> artifactProperties,
                                                  LockFactory runtimeLockFactory,
                                                  MuleContextWithRegistry muleContext,
                                                  ConfigurationProperties configurationProperties,
                                                  ArtifactType artifactType,
                                                  OptionalObjectsController optionalObjectsController,
                                                  BeanDefinitionRegistry beanDefinitionRegistry,
                                                  Registry serviceLocator,
                                                  ResourceLocator resourceLocator) {
    super(muleContext, configurationProperties, artifactProperties, artifactType, optionalObjectsController,
          beanDefinitionRegistry,
          serviceLocator, resourceLocator);
    this.lazyComponentInitializer = lazyComponentInitializer;
    this.artifactProperties = artifactProperties;
    this.runtimeLockFactory = runtimeLockFactory;
  }

  @Override
  void createArtifactServices() {
    super.createArtifactServices();

    registerBeanDefinition(OBJECT_DW_EXPRESSION_LANGUAGE_ADAPTER,
                           getBeanDefinition(LazyDataWeaveExtendedExpressionLanguageAdaptorFactoryBean.class));
    registerBeanDefinition(OBJECT_CONNECTIVITY_TESTER_FACTORY, getBeanDefinition(NoOpConnectivityTesterFactory.class));

    registerConstantBeanDefinition(CONNECTIVITY_TESTING_SERVICE_KEY,
                                   new LazyConnectivityTestingService(lazyComponentInitializer, () -> getRegistry()
                                       .<ConnectivityTestingService>lookupObject(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)));
    registerBeanDefinition(NON_LAZY_CONNECTIVITY_TESTING_SERVICE, getBeanDefinition(DefaultConnectivityTestingService.class));

    registerConstantBeanDefinition(METADATA_SERVICE_KEY,
                                   new LazyMetadataService(lazyComponentInitializer, () -> getRegistry()
                                       .<MetadataService>lookupObject(NON_LAZY_METADATA_SERVICE_KEY)));
    registerBeanDefinition(NON_LAZY_METADATA_SERVICE_KEY, getBeanDefinition(MuleMetadataService.class));

    registerConstantBeanDefinition(VALUE_PROVIDER_SERVICE_KEY,
                                   new LazyValueProviderService(lazyComponentInitializer, () -> getRegistry()
                                       .<ValueProviderService>lookupObject(NON_LAZY_VALUE_PROVIDER_SERVICE),
                                                                () -> getMuleContext().getConfigurationComponentLocator()));
    registerBeanDefinition(NON_LAZY_VALUE_PROVIDER_SERVICE, getBeanDefinition(MuleValueProviderService.class));

    registerConstantBeanDefinition(SAMPLE_DATA_SERVICE_KEY,
                                   new LazySampleDataService(lazyComponentInitializer, () -> getRegistry()
                                       .<SampleDataService>lookupObject(NON_LAZY_SAMPLE_DATA_SERVICE)));
    registerBeanDefinition(NON_LAZY_SAMPLE_DATA_SERVICE, getBeanDefinition(MuleSampleDataService.class));

    registerConstantBeanDefinition(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, lazyComponentInitializer);

    String sharedPartitionedPersistentObjectStorePath = artifactProperties.get(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH);
    if (sharedPartitionedPersistentObjectStorePath != null) {
      // We need to first define this service so it would be later initialized
      registerBeanDefinition(SHARED_PERSISTENT_OBJECT_STORE_KEY, getBeanDefinition(SharedPartitionedPersistentObjectStore.class));
      registerConstantBeanDefinition(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                     new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionedPersistentObjectStorePath),
                                                                                  runtimeLockFactory));
      MuleObjectStoreManager osm = new MuleObjectStoreManager();
      osm.setBasePersistentStoreKey(SHARED_PERSISTENT_OBJECT_STORE_KEY);
      osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
      registerConstantBeanDefinition(LAZY_MULE_OBJECT_STORE_MANAGER, osm);

      registerBeanDefinition(DEFAULT_METADATA_CACHE_MANAGER_KEY, getBeanDefinition(DefaultPersistentMetadataCacheManager.class));
      registerConstantBeanDefinition(METADATA_CACHE_MANAGER_KEY,
                                     new DelegateMetadataCacheManager(() -> {
                                       DefaultPersistentMetadataCacheManager defaultPersistentMetadataCacheManager = getRegistry()
                                           .<DefaultPersistentMetadataCacheManager>lookupObject(DEFAULT_METADATA_CACHE_MANAGER_KEY);
                                       defaultPersistentMetadataCacheManager.setLockFactory(runtimeLockFactory);
                                       defaultPersistentMetadataCacheManager.setObjectStoreManager(getRegistry()
                                           .<ObjectStoreManager>lookupObject(LAZY_MULE_OBJECT_STORE_MANAGER));
                                       return defaultPersistentMetadataCacheManager;
                                     }));
    }
  }

  protected MuleRegistry getRegistry() {
    return getMuleContext().getRegistry();
  }

}
