/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.tck.junit4.matcher.metadata.MetadataKeyResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_SERVICE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_SERVICE)
public class ConfigMetadataKeysTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private MetadataService metadataManager;

  @Override
  protected String getConfigFile() {
    return "vegan-config.xml";
  }

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Test
  public void getMetadataKeysForConfig() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("apple").build());
    assertThat(metadataKeysResult, isSuccess());
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.size(), is(2));
    assertThat(metadataKeys.get("HarvestedKeys").size(), is(1));
    assertThat(metadataKeys.get("AppleKeys").size(), is(1));
  }

  @Test
  public void getMetadataKeysForConfigWithoutResolvers() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("banana").build());
    assertThat(metadataKeysResult, isSuccess());
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
