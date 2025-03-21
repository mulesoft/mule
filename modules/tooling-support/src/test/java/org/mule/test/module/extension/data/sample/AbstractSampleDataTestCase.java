/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.test.data.sample.extension.SampleDataExtension.EXTENSION_NAME;
import static org.mule.test.module.extension.internal.util.extension.data.sample.SampleDataTestUtils.exceptionMatcher;

import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public abstract class AbstractSampleDataTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String EXPECTED_PAYLOAD = "my payload";
  protected static final String EXPECTED_ATTRIBUTES = "my attributes";
  protected static final String CONF_PREFIX = "from-conf-";
  protected static final String NULL_VALUE = "<<null>>";

  @Inject
  @Named(SAMPLE_DATA_SERVICE_KEY)
  private SampleDataService sampleDataService;

  @Inject
  private ExtensionManager extensionManager;

  @Rule
  public ExpectedException expectedException = none();

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

  protected void assertMessage(Message message, String payload, String attributes) {
    assertThat(message.getPayload().getValue(), equalTo(payload));
    assertThat(message.getPayload().getDataType().getMediaType().matches(APPLICATION_JSON), is(true));
    assertThat(message.getAttributes().getValue(), equalTo(attributes));
    assertThat(message.getAttributes().getDataType().getMediaType().matches(APPLICATION_XML), is(true));
  }

  protected Message getOperationSampleByLocation(String flowName) throws SampleDataException {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return sampleDataService.getSampleData(location);
  }

  protected Message getSourceSampleByLocation(String flowName) throws SampleDataException {
    Location location = Location.builder().globalName(flowName).addSourcePart().build();
    return sampleDataService.getSampleData(location);
  }

  protected Message getSampleByComponentName(String componentName,
                                             Map<String, Object> parameters,
                                             String configName)
      throws SampleDataException {
    return sampleDataService.getSampleData(
                                           EXTENSION_NAME,
                                           componentName,
                                           parameters,
                                           getConfigurationSupplier(configName));
  }

  private Supplier<Optional<ConfigurationInstance>> getConfigurationSupplier(String configName) {
    if (configName == null) {
      return Optional::empty;
    }

    return (CheckedSupplier<Optional<ConfigurationInstance>>) () -> of(extensionManager.getConfiguration(configName,
                                                                                                         testEvent()));
  }

  protected void expectSampleDataException(String failureCode) {
    expectedException.expect(SampleDataException.class);
    expectedException.expect(exceptionMatcher(failureCode));
  }

  protected void expectSampleDataException(Class<? extends SampleDataException> sampleDataExceptionClass, String failureCode,
                                           String errorMessage, Optional<Class<? extends Throwable>> causeClass) {
    expectedException.expect(sampleDataExceptionClass);
    expectedException.expect(exceptionMatcher(failureCode));
    expectedException.expectMessage(errorMessage);
    if (causeClass.isPresent()) {
      expectedException.expectCause(instanceOf(causeClass.get()));
    } else {
      expectedException.expectCause(nullValue(Throwable.class));
    }
  }

  protected Map<String, Object> getDefaultParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("payload", "my payload");
    params.put("attributes", "my attributes");

    return params;
  }

  protected Map<String, Object> getGroupParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("groupParameter", "my payload");
    params.put("optionalParameter", "my attributes");

    return params;
  }
}
