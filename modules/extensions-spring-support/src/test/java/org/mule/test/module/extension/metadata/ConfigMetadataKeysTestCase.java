/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigMetadataKeysTestCase extends AbstractExtensionFunctionalTestCase {

  public static final String VEGAN_CONFIG_XML = "vegan-config.xml";
  public static final String HARVEST_APPLE_KEY_RESOLVER_KEYS = "HarvestAppleKeyResolver.keys";
  @Inject
  private MetadataService metadataManager;

  private File keysOverride;

  @Override
  protected String getConfigFile() {
    return VEGAN_CONFIG_XML;
  }

  @Before
  public void setUp() throws Exception {
    keysOverride = new File(getClass().getClassLoader().getResource(HARVEST_APPLE_KEY_RESOLVER_KEYS).getPath());
    FileUtils.writeStringToFile(keysOverride, "");
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.writeStringToFile(keysOverride, "");
  }

  @Test
  public void getMetadataKeysForConfig() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("apple").build());
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.size(), is(1));
    Set<MetadataKey> harvestedKeys = metadataKeys.get("HarvestedKeys");
    assertThat(harvestedKeys.size(), is(1));
    assertThat(harvestedKeys.iterator().next().getId(), is("HARVESTED"));
  }

  @Test
  public void getMetadataKeysForConfigFromResources() throws Exception {
    FileUtils.writeStringToFile(keysOverride, "LOADED,OVERRIDE,HARVEST");

    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("apple").build());
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.size(), is(1));
    Set<MetadataKey> harvestedKeys = metadataKeys.get("HarvestedKeys");
    assertThat(harvestedKeys.size(), is(3));
    assertThat(harvestedKeys.stream().map(MetadataKey::getId).collect(toList()),
               containsInAnyOrder("LOADED", "OVERRIDE", "HARVEST"));
  }

  @Test
  public void getMetadataKeysForConfigWithoutResolvers() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("banana").build());
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.isEmpty(), is(true));
  }

  private Map<String, Set<MetadataKey>> getKeyMapFromContainer(MetadataResult<MetadataKeysContainer> metadataKeysResult) {
    return metadataKeysResult.get()
        .getCategories()
        .stream()
        .collect(Collectors.toMap(resolver -> resolver, resolver -> metadataKeysResult.get().getKeys(resolver).get()));
  }
}
