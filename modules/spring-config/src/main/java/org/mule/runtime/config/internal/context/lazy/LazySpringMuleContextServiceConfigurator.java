/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.internal.bean.lazy.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.bean.lazy.LazySampleDataService.NON_LAZY_SAMPLE_DATA_SERVICE;
import static org.mule.runtime.config.internal.bean.lazy.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext.SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEMORY_MANAGEMENT_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTER_FACTORY;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore.SHARED_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory.METADATA_CACHE_ID_GENERATOR_KEY;
import static org.mule.runtime.metadata.internal.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.config.internal.bean.lazy.LazyConnectivityTestingService;
import org.mule.runtime.config.internal.bean.lazy.LazyMetadataService;
import org.mule.runtime.config.internal.bean.lazy.LazySampleDataService;
import org.mule.runtime.config.internal.bean.lazy.LazyValueProviderService;
import org.mule.runtime.config.internal.context.SpringMuleContextServiceConfigurator;
import org.mule.runtime.config.internal.lazy.NoOpConnectivityTesterFactory;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.metadata.internal.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.metadata.internal.cache.lazy.DelegateMetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.internal.cache.lazy.DelegateMetadataCacheManager;
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
  private static final String DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY = "_defaultMetadataCacheIdGenerator";
  private static final String LAZY_MULE_OBJECT_STORE_MANAGER = "_muleLazyObjectStoreManager";

  private final LazyComponentInitializerAdapter lazyComponentInitializer;
  private final LockFactory runtimeLockFactory;

  public LazySpringMuleContextServiceConfigurator(LazyComponentInitializerAdapter lazyComponentInitializer,
                                                  Map<String, String> artifactProperties,
                                                  boolean addToolingObjectsToRegistry,
                                                  LockFactory runtimeLockFactory,
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
          addToolingObjectsToRegistry,
          artifactType.getArtifactType(),
          artifactAst,
          beanDefinitionRegistry,
          serviceLocator, resourceLocator, memoryManagementService);
    this.lazyComponentInitializer = lazyComponentInitializer;
    this.runtimeLockFactory = runtimeLockFactory;
  }

  @Override
  protected void createArtifactServices() {
    super.createArtifactServices();

    registerBeanDefinition(OBJECT_CONNECTIVITY_TESTER_FACTORY, getBeanDefinition(NoOpConnectivityTesterFactory.class));
    registerConstantBeanDefinition(MULE_MEMORY_MANAGEMENT_SERVICE, getMemoryManagementService(), true);

    if (isAddToolingObjectsToRegistry()) {
      registerConstantBeanDefinition(CONNECTIVITY_TESTING_SERVICE_KEY,
                                     new LazyConnectivityTestingService(lazyComponentInitializer, () -> getRegistry()
                                         .<ConnectivityTestingService>lookupObject(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)),
                                     true);
      registerBeanDefinition(NON_LAZY_CONNECTIVITY_TESTING_SERVICE, getBeanDefinition(DefaultConnectivityTestingService.class));

      registerConstantBeanDefinition(METADATA_SERVICE_KEY,
                                     new LazyMetadataService(lazyComponentInitializer, () -> getRegistry()
                                         .<MetadataService>lookupObject(NON_LAZY_METADATA_SERVICE_KEY)),
                                     true);
      registerBeanDefinition(NON_LAZY_METADATA_SERVICE_KEY, getBeanDefinition(MuleMetadataService.class));

      registerConstantBeanDefinition(VALUE_PROVIDER_SERVICE_KEY,
                                     new LazyValueProviderService(lazyComponentInitializer, () -> getRegistry()
                                         .<ValueProviderService>lookupObject(NON_LAZY_VALUE_PROVIDER_SERVICE)),
                                     true);
      registerBeanDefinition(NON_LAZY_VALUE_PROVIDER_SERVICE, getBeanDefinition(MuleValueProviderService.class));

      registerConstantBeanDefinition(SAMPLE_DATA_SERVICE_KEY,
                                     new LazySampleDataService(lazyComponentInitializer, () -> getRegistry()
                                         .<SampleDataService>lookupObject(NON_LAZY_SAMPLE_DATA_SERVICE)),
                                     true);
      registerBeanDefinition(NON_LAZY_SAMPLE_DATA_SERVICE, getBeanDefinition(MuleSampleDataService.class));

      registerBeanDefinition(DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY,
                             getBeanDefinition(ModelBasedMetadataCacheIdGeneratorFactory.class));
      registerConstantBeanDefinition(METADATA_CACHE_ID_GENERATOR_KEY,
                                     new DelegateMetadataCacheIdGeneratorFactory(() -> getRegistry()
                                         .<ModelBasedMetadataCacheIdGeneratorFactory>lookupObject(DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY)),
                                     true);
    }

    registerConstantBeanDefinition(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, lazyComponentInitializer, true);

    String sharedPartitionedPersistentObjectStorePath =
        getArtifactProperties().get(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH);
    if (sharedPartitionedPersistentObjectStorePath != null) {
      // We need to first define this service so it would be later initialized
      registerBeanDefinition(SHARED_PERSISTENT_OBJECT_STORE_KEY, getBeanDefinition(SharedPartitionedPersistentObjectStore.class));
      registerConstantBeanDefinition(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                     new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionedPersistentObjectStorePath),
                                                                                  runtimeLockFactory),
                                     true);
      MuleObjectStoreManager osm = new MuleObjectStoreManager();
      osm.setBasePersistentStoreKey(SHARED_PERSISTENT_OBJECT_STORE_KEY);
      osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
      registerConstantBeanDefinition(LAZY_MULE_OBJECT_STORE_MANAGER, osm, true);

      if (isAddToolingObjectsToRegistry()) {
        registerBeanDefinition(DEFAULT_METADATA_CACHE_MANAGER_KEY,
                               getBeanDefinition(DefaultPersistentMetadataCacheManager.class));
        registerConstantBeanDefinition(METADATA_CACHE_MANAGER_KEY,
                                       new DelegateMetadataCacheManager(() -> {
                                         DefaultPersistentMetadataCacheManager defaultPersistentMetadataCacheManager =
                                             getRegistry()
                                                 .<DefaultPersistentMetadataCacheManager>lookupObject(DEFAULT_METADATA_CACHE_MANAGER_KEY);
                                         defaultPersistentMetadataCacheManager.setLockFactory(runtimeLockFactory);
                                         defaultPersistentMetadataCacheManager.setObjectStoreManager(getRegistry()
                                             .<ObjectStoreManager>lookupObject(LAZY_MULE_OBJECT_STORE_MANAGER));
                                         return defaultPersistentMetadataCacheManager;
                                       }), true);
      }
    }
  }

  protected MuleRegistry getRegistry() {
    return getMuleContext().getRegistry();
  }

}
