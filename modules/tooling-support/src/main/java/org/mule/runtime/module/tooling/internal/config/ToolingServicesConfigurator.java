/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.internal.bean.lazy.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.bean.lazy.LazySampleDataService.NON_LAZY_SAMPLE_DATA_SERVICE;
import static org.mule.runtime.config.internal.bean.lazy.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext.SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH;
import static org.mule.runtime.config.internal.context.lazy.LazySpringMuleContextServiceConfigurator.LAZY_MULE_OBJECT_STORE_MANAGER;
import static org.mule.runtime.config.internal.context.lazy.LazySpringMuleContextServiceConfigurator.LAZY_MULE_RUNTIME_LOCK_FACTORY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_ADD_TOOLING_OBJECTS_TO_REGISTRY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory.METADATA_CACHE_ID_GENERATOR_KEY;
import static org.mule.runtime.metadata.internal.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;

import static java.lang.Boolean.parseBoolean;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.config.api.dsl.model.metadata.ModelBasedMetadataCacheIdGeneratorFactory;
import org.mule.runtime.config.internal.bean.lazy.LazyConnectivityTestingService;
import org.mule.runtime.config.internal.bean.lazy.LazyMetadataService;
import org.mule.runtime.config.internal.bean.lazy.LazySampleDataService;
import org.mule.runtime.config.internal.bean.lazy.LazyValueProviderService;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.metadata.internal.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.module.tooling.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.module.tooling.internal.data.sample.MuleSampleDataService;
import org.mule.runtime.module.tooling.internal.metadata.cache.lazy.DelegateMetadataCacheIdGeneratorFactory;
import org.mule.runtime.module.tooling.internal.metadata.cache.lazy.DelegateMetadataCacheManager;
import org.mule.runtime.module.tooling.internal.value.MuleValueProviderService;


public class ToolingServicesConfigurator implements ServiceConfigurator {

  private static final String DEFAULT_METADATA_CACHE_MANAGER_KEY = "_defaultPersistentMetadataCacheManager";
  private static final String DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY = "_defaultMetadataCacheIdGenerator";

  @Override
  public void configure(CustomizationService customizationService) {
    if (!parseBoolean(customizationService.getArtifactProperties().get(MULE_ADD_TOOLING_OBJECTS_TO_REGISTRY))) {
      return;
    }

    customizationService.registerCustomServiceClass(METADATA_SERVICE_KEY,
                                                    MuleMetadataService.class,
                                                    false);
    customizationService.registerCustomServiceClass(VALUE_PROVIDER_SERVICE_KEY,
                                                    MuleValueProviderService.class,
                                                    false);
    customizationService.registerCustomServiceClass(SAMPLE_DATA_SERVICE_KEY,
                                                    MuleSampleDataService.class,
                                                    false);
    customizationService.registerCustomServiceClass(CONNECTIVITY_TESTING_SERVICE_KEY,
                                                    DefaultConnectivityTestingService.class,
                                                    false);
    customizationService.registerCustomServiceClass(METADATA_CACHE_MANAGER_KEY,
                                                    DefaultPersistentMetadataCacheManager.class,
                                                    false);
    customizationService.registerCustomServiceClass(METADATA_CACHE_ID_GENERATOR_KEY,
                                                    ModelBasedMetadataCacheIdGeneratorFactory.class,
                                                    false);

    if (parseBoolean(customizationService.getArtifactProperties().get(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY))) {
      configureLazyToolingServices(customizationService);
    }
  }

  private void configureLazyToolingServices(CustomizationService customizationService) {
    customizationService.registerCustomServiceImpl(CONNECTIVITY_TESTING_SERVICE_KEY,
                                                   new LazyConnectivityTestingService(registry -> registry
                                                       .<ConnectivityTestingService>lookupByName(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)
                                                       .get()),
                                                   false);
    customizationService.registerCustomServiceClass(NON_LAZY_CONNECTIVITY_TESTING_SERVICE,
                                                    DefaultConnectivityTestingService.class, false);

    customizationService.registerCustomServiceImpl(METADATA_SERVICE_KEY,
                                                   new LazyMetadataService(registry -> registry
                                                       .<MetadataService>lookupByName(NON_LAZY_METADATA_SERVICE_KEY)
                                                       .get()),
                                                   false);
    customizationService.registerCustomServiceClass(NON_LAZY_METADATA_SERVICE_KEY, MuleMetadataService.class, false);

    customizationService.registerCustomServiceImpl(VALUE_PROVIDER_SERVICE_KEY,
                                                   new LazyValueProviderService(registry -> registry
                                                       .<ValueProviderService>lookupByName(NON_LAZY_VALUE_PROVIDER_SERVICE)
                                                       .get()),
                                                   false);
    customizationService.registerCustomServiceClass(NON_LAZY_VALUE_PROVIDER_SERVICE, MuleValueProviderService.class, false);

    customizationService.registerCustomServiceImpl(SAMPLE_DATA_SERVICE_KEY,
                                                   new LazySampleDataService(registry -> registry
                                                       .<SampleDataService>lookupByName(NON_LAZY_SAMPLE_DATA_SERVICE)
                                                       .get()),
                                                   false);
    customizationService.registerCustomServiceClass(NON_LAZY_SAMPLE_DATA_SERVICE, MuleSampleDataService.class, false);

    customizationService.registerCustomServiceClass(DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY,
                                                    ModelBasedMetadataCacheIdGeneratorFactory.class, false);
    customizationService.registerCustomServiceImpl(METADATA_CACHE_ID_GENERATOR_KEY,
                                                   new DelegateMetadataCacheIdGeneratorFactory(registry -> registry
                                                       .<ModelBasedMetadataCacheIdGeneratorFactory>lookupByName(DEFAULT_METADATA_CACHE_ID_GENERATOR_KEY)
                                                       .get()),
                                                   false);

    if (customizationService.getArtifactProperties().get(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH) != null) {
      customizationService.registerCustomServiceClass(DEFAULT_METADATA_CACHE_MANAGER_KEY,
                                                      DefaultPersistentMetadataCacheManager.class,
                                                      false);
      customizationService.registerCustomServiceImpl(METADATA_CACHE_MANAGER_KEY,
                                                     new DelegateMetadataCacheManager(registry -> {
                                                       DefaultPersistentMetadataCacheManager defaultPersistentMetadataCacheManager =
                                                           registry
                                                               .<DefaultPersistentMetadataCacheManager>lookupByName(DEFAULT_METADATA_CACHE_MANAGER_KEY)
                                                               .get();
                                                       defaultPersistentMetadataCacheManager.setLockFactory(registry
                                                           .<LockFactory>lookupByName(LAZY_MULE_RUNTIME_LOCK_FACTORY)
                                                           .get());
                                                       defaultPersistentMetadataCacheManager.setObjectStoreManager(registry
                                                           .<ObjectStoreManager>lookupByName(LAZY_MULE_OBJECT_STORE_MANAGER)
                                                           .get());

                                                       return defaultPersistentMetadataCacheManager;
                                                     }),
                                                     false);
    }
  }

}
