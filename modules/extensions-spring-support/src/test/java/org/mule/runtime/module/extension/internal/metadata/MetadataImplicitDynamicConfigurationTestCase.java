/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import org.junit.Test;

public class MetadataImplicitDynamicConfigurationTestCase extends MetadataExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return METADATA_TEST_DYNAMIC_IMPLICIT_CONFIGURATION;
  }

  @Test
  public void resolveMetadataWithImplicitDynamicConfig() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor> metadataResult = metadataManager.getMetadata(componentId,
                                                                                                   PERSON_METADATA_KEY);
    assertFailure(metadataResult, "Configuration used for Metadata fetch cannot be dynamic", FailureCode.INVALID_CONFIGURATION,
                  MetadataResolvingException.class.getName());
  }
}
