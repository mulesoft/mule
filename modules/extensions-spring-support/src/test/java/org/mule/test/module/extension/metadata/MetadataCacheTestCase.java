/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.internal.metadata.cache.DefaultPersistentMetadataCacheManager.PERSISTENT_METADATA_SERVICE_CACHE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.AGE_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.BRAND_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.NAME_VALUE;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetadataCacheTestCase extends AbstractMetadataOperationTestCase {

  private static final String OUTPUT_AND_METADATA_KEY_CACHE_ID = "1874947571-1840879217-380895431-1745289126-135479212676912086";
  private static final String OUTPUT_METADATA_WITHOUT_KEY_CACHE_ID =
      "1874947571-1840879217-1768400440-174528912655077923476912086";
  private static final String CONTENT_AND_OUTPUT_CACHE_ID = "1874947571-1840879217-1768400440-174528912655077923476912086";

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  public MetadataCacheTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return RUNTIME_METADATA_CONFIG;
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return false;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return false;
  }

  @Before
  public void setUp() throws Exception {
    try {
      getMetadataObjectStore().clear();
    } catch (Exception ignored) {
    }
  }

  @After
  public void tearDown() throws Exception {
    try {
      getMetadataObjectStore().clear();
    } catch (Exception ignored) {
    }
  }

  @Test
  public void multipleCaches() throws Exception {
    // using config
    setLocation(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER);
    metadataService.getMetadataKeys(location);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    setLocation(OUTPUT_METADATA_WITHOUT_KEY_PARAM);
    getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);

    // using alternative-config
    setLocation(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    // re-use same key
    setLocation(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    List<String> actualKeys = getMetadataObjectStore().allKeys();
    assertThat(actualKeys, hasSize(3));
    assertThat(actualKeys,
               hasItems(OUTPUT_AND_METADATA_KEY_CACHE_ID, OUTPUT_METADATA_WITHOUT_KEY_CACHE_ID, CONTENT_AND_OUTPUT_CACHE_ID));
  }

  protected void setLocation(String outputAndMetadataKeyCacheResolver) {
    location = Location.builder().globalName(outputAndMetadataKeyCacheResolver).addProcessorsPart().addIndexPart(0).build();
  }

  @Test
  public void elementsAreStoredInCaches() throws Exception {
    // using config
    setLocation(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER);
    metadataService.getMetadataKeys(location);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    // using alternative-config
    setLocation(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG);
    getSuccessComponentDynamicMetadata();

    MetadataCache configCache = getMetadataObjectStore().retrieve(OUTPUT_AND_METADATA_KEY_CACHE_ID);

    assertThat(configCache.get(AGE).get(), is(AGE_VALUE));
    assertThat(configCache.get(NAME).get(), is(NAME_VALUE));
    assertThat(configCache.get(BRAND).get(), is(BRAND_VALUE));

    MetadataCache alternativeConfigCache = getMetadataObjectStore().retrieve(CONTENT_AND_OUTPUT_CACHE_ID);
    assertThat(alternativeConfigCache.get(BRAND).get(), is(BRAND_VALUE));
  }

  @Test
  public void disposeCacheForConfig() throws Exception {
    // using config
    setLocation(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    // using alternative-config
    setLocation(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG);
    getSuccessComponentDynamicMetadata();

    assertThat(getMetadataObjectStore().allKeys(), hasSize(2));
    getMetadataObjectStore().retrieve(OUTPUT_AND_METADATA_KEY_CACHE_ID);
    getMetadataObjectStore().retrieve(CONTENT_AND_OUTPUT_CACHE_ID);

    metadataService.disposeCache(CONTENT_AND_OUTPUT_CACHE_ID);

    assertThat(getMetadataObjectStore().allKeys(), hasSize(1));
    getMetadataObjectStore().retrieve(OUTPUT_AND_METADATA_KEY_CACHE_ID);
    try {
      getMetadataObjectStore().retrieve(CONTENT_AND_OUTPUT_CACHE_ID);
      fail();
    } catch (ObjectDoesNotExistException success) {
    }
  }

  @Test
  public void disposeCacheForPartialId() throws Exception {
    // using config
    setLocation(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    // using alternative-config
    setLocation(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG);
    getSuccessComponentDynamicMetadata();

    assertThat(getMetadataObjectStore().allKeys(), hasSize(2));
    getMetadataObjectStore().retrieve(OUTPUT_AND_METADATA_KEY_CACHE_ID);
    getMetadataObjectStore().retrieve(CONTENT_AND_OUTPUT_CACHE_ID);

    metadataService.disposeCache("1874947571-1840879217");

    assertThat(getMetadataObjectStore().allKeys(), hasSize(0));
    try {
      getMetadataObjectStore().retrieve(OUTPUT_AND_METADATA_KEY_CACHE_ID);
      fail();
    } catch (ObjectDoesNotExistException success) {
    }
    try {
      getMetadataObjectStore().retrieve(CONTENT_AND_OUTPUT_CACHE_ID);
      fail();
    } catch (ObjectDoesNotExistException success) {
    }
  }

  private ObjectStore<MetadataCache> getMetadataObjectStore() {
    return objectStoreManager.getObjectStore(PERSISTENT_METADATA_SERVICE_CACHE);
  }

}
