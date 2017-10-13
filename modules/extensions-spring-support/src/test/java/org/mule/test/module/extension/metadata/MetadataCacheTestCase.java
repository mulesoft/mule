/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.AGE_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.BRAND_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.NAME_VALUE;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataService;

import javax.inject.Inject;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

public class MetadataCacheTestCase extends AbstractMetadataOperationTestCase {

  private static final String CONFIG = "config";
  private static final String ALTERNATIVE_CONFIG = "alternative-config";

  @Inject
  private MetadataService metadataManager;

  public MetadataCacheTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return RUNTIME_METADATA_CONFIG;
  }

  @Override
  public boolean enableLazyInit() {
    return false;
  }

  @Override
  public boolean disableXmlValidations() {
    return false;
  }

  @Test
  public void multipleCaches() throws Exception {
    Map<String, ? extends MetadataCache> caches = getMetadataCaches(metadataManager);
    caches.keySet().forEach(metadataManager::disposeCache);

    // using config
    location = Location.builder().globalName(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    location = Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEY_PARAM).addProcessorsPart().addIndexPart(0).build();
    getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);

    // using alternative-config
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG).addProcessorsPart()
        .addIndexPart(0).build();
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    caches = getMetadataCaches(metadataManager);

    assertThat(caches.keySet(), hasSize(2));
    assertThat(caches.keySet(), hasItems(CONFIG, ALTERNATIVE_CONFIG));
  }

  @Test
  public void elementsAreStoredInCaches() throws Exception {
    // using config
    location = Location.builder().globalName(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    metadataService.getMetadataKeys(location);
    getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    // using alternative-config
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG).addProcessorsPart()
        .addIndexPart(0).build();
    getSuccessComponentDynamicMetadata();

    MetadataCache configCache = getMetadataCaches(metadataManager).get(CONFIG);

    assertThat(configCache.get(AGE).get(), is(AGE_VALUE));
    assertThat(configCache.get(NAME).get(), is(NAME_VALUE));
    assertThat(configCache.get(BRAND).get(), is(BRAND_VALUE));

    MetadataCache alternativeConfigCache = getMetadataCaches(metadataManager).get(ALTERNATIVE_CONFIG);
    assertThat(alternativeConfigCache.get(BRAND).get(), is(BRAND_VALUE));
  }

  private Map<String, ? extends MetadataCache> getMetadataCaches(MetadataService metadataManager) {
    try {
      Method getMetadataCachesMethod = metadataManager.getClass().getMethod("getMetadataCaches");
      return (Map<String, ? extends MetadataCache>) getMetadataCachesMethod.invoke(metadataManager);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot obtain metadata caches", e);
    }
  }

}
