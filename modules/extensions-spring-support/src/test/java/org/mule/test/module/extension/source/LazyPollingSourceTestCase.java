/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_SERVICE;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.isA;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Test;

import org.hamcrest.Matcher;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Features({@Feature(SOURCES), @Feature(SDK_TOOLING_SUPPORT)})
@Stories({@Story(POLLING), @Story(METADATA_SERVICE)})
public class LazyPollingSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Inject
  @Named(METADATA_SERVICE_KEY)
  MetadataService metadataService;

  @Override
  protected String getConfigFile() {
    return "source/polling-source-config-without-scheduling-strategy.xml";
  }

  @Test
  public void resolvePollingSourceMetadata() {
    MetadataResult<ComponentMetadataDescriptor<SourceModel>> sourceMetadata =
        metadataService.getSourceMetadata(Location.builder().globalName("polling-source").addSourcePart().build());
    assertThat(sourceMetadata.get().getModel().getOutput().getType(), (Matcher) isA(StringType.class));
  }
}
