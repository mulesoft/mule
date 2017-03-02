/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import org.mule.runtime.api.metadata.MetadataKey;

import org.junit.Test;

public class MetadataNoRefStaticConfigurationTestCase extends MetadataExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return METADATA_TEST_STATIC_NO_REF_CONFIGURATION;
  }

  @Test
  public void resolveMetadataWithNoRefStaticConfig() throws Exception {
    location = builder().globalName(RESOLVER_WITH_IMPLICIT_STATIC_CONFIG).addProcessorsPart()
        .addIndexPart(0).build();
    MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
    getSuccessComponentDynamicMetadata(key);
  }
}
