/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.metadata;

import static org.mule.runtime.api.component.location.Location.builder;

import org.junit.Test;

public class MetadataNoRefStaticConfigurationTestCase extends AbstractMetadataOperationTestCase {

  public MetadataNoRefStaticConfigurationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST_STATIC_NO_REF_CONFIGURATION;
  }

  @Test
  public void resolveMetadataWithNoRefStaticConfig() throws Exception {
    location = builder().globalName(RESOLVER_WITH_IMPLICIT_STATIC_CONFIG).addProcessorsPart()
        .addIndexPart(0).build();
    getSuccessComponentDynamicMetadata(LOCATION_MULTILEVEL_KEY);
  }
}
