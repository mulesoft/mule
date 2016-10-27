/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.metadata.ConfigurationId;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class ConfigMetadataKeysTestCase extends ExtensionFunctionalTestCase {

  private MuleMetadataService metadataManager;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {VeganExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "vegan-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    metadataManager = muleContext.getRegistry().lookupObject(MuleMetadataService.class);
  }

  @Test
  public void getMetadataKeysForConfig() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(new ConfigurationId("apple"));
    assertThat(metadataKeysResult.isSuccess(), is(true));
    final Map<String, Set<MetadataKey>> metadataKeys = getKeyMapFromContainer(metadataKeysResult);
    assertThat(metadataKeys.size(), is(2));
    assertThat(metadataKeys.get("AppleKeys").size(), is(1));
    assertThat(metadataKeys.get("HarvestedKeys").size(), is(1));
  }

  @Test
  public void getMetadataKeysForConfigWithoutResolvers() throws Exception {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataManager.getMetadataKeys(new ConfigurationId("banana"));
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
