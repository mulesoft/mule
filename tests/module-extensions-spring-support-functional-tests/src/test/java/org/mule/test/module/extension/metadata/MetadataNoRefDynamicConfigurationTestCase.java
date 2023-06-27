/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.MetadataComponent.COMPONENT;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class MetadataNoRefDynamicConfigurationTestCase extends AbstractMetadataOperationTestCase {

  public MetadataNoRefDynamicConfigurationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST_DYNAMIC_NO_REF_CONFIGURATION;
  }

  @Test
  public void resolveMetadataWithNoRefDynamicConfig() throws Exception {
    location = builder().globalName(RESOLVER_WITH_IMPLICIT_DYNAMIC_CONFIG).addProcessorsPart().addIndexPart(0).build();
    MetadataKey key = newKey(AMERICA).withChild(newKey(USA).withChild(newKey(SAN_FRANCISCO))).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataResult = getComponentDynamicMetadata(key);
    assertFailureResult(metadataResult, 1);
    assertMetadataFailure(metadataResult.getFailures().get(0),
                          "Configuration used for Metadata fetch cannot be dynamic",
                          INVALID_CONFIGURATION,
                          MetadataResolvingException.class.getName(),
                          COMPONENT,
                          "");
  }
}
