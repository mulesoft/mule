/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.isA;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.hamcrest.Matcher;
import org.junit.Test;

public class LazyPollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Inject
  @Named(METADATA_SERVICE_KEY)
  MetadataService metadataService;

  @Override
  protected String getConfigFile() {
    return "polling-source-config-without-scheduling-strategy.xml";
  }

  @Test
  public void resolvePollingSourceMetadata() {
    MetadataResult<ComponentMetadataDescriptor<SourceModel>> sourceMetadata =
        metadataService.getSourceMetadata(Location.builder().globalName("polling-source").addSourcePart().build());
    assertThat(sourceMetadata.get().getModel().getOutput().getType(), (Matcher) isA(StringType.class));
  }
}
