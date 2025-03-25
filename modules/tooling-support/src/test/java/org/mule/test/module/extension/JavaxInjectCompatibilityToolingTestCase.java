/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVAX_INJECT_COMPATIBILITY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.javaxinject.JavaxInjectCompatibilityTestExtension.JAVAX_INJECT_COMPATIBILITY_TEST_EXTENSION;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(JAVAX_INJECT_COMPATIBILITY)
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public class JavaxInjectCompatibilityToolingTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  @Named(SAMPLE_DATA_SERVICE_KEY)
  private SampleDataService sampleDataService;

  @Inject
  @Named(VALUE_PROVIDER_SERVICE_KEY)
  private ValueProviderService valueProviderService;

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

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "inject/javax-inject-compatibility-config.xml";
  }

  @Test
  public void sampleData() throws SampleDataException {
    final Message sampleData =
        sampleDataService.getSampleData(JAVAX_INJECT_COMPATIBILITY_TEST_EXTENSION, "execute", emptyMap(), Optional::empty);

    assertThat(sampleData.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void valueProviders() {
    Location location = Location.builder().globalName("valueProvider").addProcessorsPart().addIndexPart(0).build();
    final ValueResult values = valueProviderService.getFieldValues(location, "param", null);

    assertThat(values.getValues(), iterableWithSize(1));
    assertThat(values.getValues().iterator().next().getId(), is(defaultCharset().name()));
  }

}
