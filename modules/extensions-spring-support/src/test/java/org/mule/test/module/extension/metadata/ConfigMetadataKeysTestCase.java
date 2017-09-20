/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;

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

public class ConfigMetadataKeysTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private MetadataService metadataManager;

  @Override
  protected String getConfigFile() {
    return "vegan-config.xml";
  }

  @Test
  public void getMetadataKeysForConfig() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(builder().globalName("apple").build());
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.size(), is(2));
    assertThat(metadataKeys.get("AppleKeys").size(), is(1));
    assertThat(metadataKeys.get("HarvestedKeys").size(), is(1));
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
