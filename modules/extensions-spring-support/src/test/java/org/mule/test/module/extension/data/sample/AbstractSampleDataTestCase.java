/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

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

  @Rule
  public ExpectedException expectedException = none();

  @Override
  public boolean enableLazyInit() {
    return false;
  }

  @Override
  public boolean disableXmlValidations() {
    return false;
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

  protected Message getOperationSample(String flowName) throws SampleDataException {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return sampleDataService.getSampleData(location);
  }

  protected Message getSourceSample(String flowName) throws SampleDataException {
    Location location = Location.builder().globalName(flowName).addSourcePart().build();
    return sampleDataService.getSampleData(location);
  }
}
