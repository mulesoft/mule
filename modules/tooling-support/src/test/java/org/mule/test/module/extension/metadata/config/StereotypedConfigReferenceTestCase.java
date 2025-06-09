/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata.config;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.tck.junit4.matcher.metadata.MetadataKeyResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_SERVICE;

import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@RunnerDelegateTo(Parameterized.class)
@Feature(SDK_TOOLING_SUPPORT)
@Story(METADATA_SERVICE)
public class StereotypedConfigReferenceTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  @Named(METADATA_SERVICE_KEY)
  protected MetadataService metadataService;

  @Parameterized.Parameter
  public String baseConfig;

  @Parameterized.Parameters(name = "{0}")
  public static String[] params() {
    return new String[] {
        "metadata/stereotype-config-reference.xml",
        "metadata/stereotype-config-reference-inverse.xml"
    };
  }

  @Override
  protected String getConfigFile() {
    return baseConfig;
  }

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

  @Test
  public void applicationInitialisesBothBeans() {
    final MetadataResult<MetadataKeysContainer> metadataKeysResult =
        metadataService.getMetadataKeys(builder().globalName("drStrange").build());
    assertThat(metadataKeysResult, isSuccess());
  }
}
