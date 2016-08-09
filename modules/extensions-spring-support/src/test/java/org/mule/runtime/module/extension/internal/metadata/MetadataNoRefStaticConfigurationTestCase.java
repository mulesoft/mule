/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;

import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class MetadataNoRefStaticConfigurationTestCase extends MetadataExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return METADATA_TEST_STATIC_NO_REF_CONFIGURATION;
  }

  @Test
  public void resolveMetadataWithNoRefStaticConfig() throws Exception {
    componentId = new ProcessorId(RESOLVER_WITH_IMPLICIT_STATIC_CONFIG, FIRST_PROCESSOR_INDEX);
    MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
    final MetadataResult<ComponentMetadataDescriptor> metadataResult = metadataManager.getMetadata(componentId, key);
    assertThat(metadataResult.isSuccess(), is(true));
  }
}
