/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.INVALID_LOCATION;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import static org.mule.sdk.api.data.sample.SampleDataException.NO_DATA_AVAILABLE;
import static org.mule.sdk.api.data.sample.SampleDataException.UNKNOWN;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SampleDataStory.SAMPLE_DATA_SERVICE;
import static org.mule.test.module.extension.internal.util.extension.data.sample.SampleDataTestUtils.exceptionMatcher;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.data.sample.ComponentSampleDataProvider;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(SDK_TOOLING_SUPPORT)
@Story(SAMPLE_DATA_SERVICE)
public class MuleSampleDataServiceTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private Location location;

  @Mock
  private ConfigurationComponentLocator componentLocator;

  @Mock(extraInterfaces = {Component.class})
  private ComponentSampleDataProvider componentSampleDataProvider;

  @Mock
  private Message message;

  private final MuleSampleDataService sampleDataService = new MuleSampleDataService();

  @Before
  public void before() throws SampleDataException {
    sampleDataService.setComponentLocator(componentLocator);
    when(componentSampleDataProvider.getSampleData()).thenReturn(message);
    when(componentLocator.find(location)).thenReturn(of((Component) componentSampleDataProvider));
  }

  @Test
  public void getSampleData() throws SampleDataException {
    assertThat(sampleDataService.getSampleData(location), is(sameInstance(message)));
    verify(componentSampleDataProvider).getSampleData();
  }

  @Test
  public void noDataFound() throws SampleDataException {
    expectSampleDataException(NO_DATA_AVAILABLE);
    when(componentSampleDataProvider.getSampleData()).thenReturn(null);
    sampleDataService.getSampleData(location);
  }

  @Test
  public void invalidLocation() throws SampleDataException {
    expectSampleDataException(INVALID_LOCATION);
    when(componentLocator.find(location)).thenReturn(empty());

    sampleDataService.getSampleData(location);
  }

  @Test
  public void resolutionException() throws SampleDataException {
    SampleDataException e = new SampleDataException("some message", UNKNOWN);
    expectedException.expect(sameInstance(e));

    when(componentSampleDataProvider.getSampleData()).thenThrow(e);
    sampleDataService.getSampleData(location);
  }

  @Test
  public void unsupportedComponent() throws SampleDataException {
    when(componentLocator.find(location)).thenReturn(of(mock(Component.class)));
    expectSampleDataException(NOT_SUPPORTED);

    sampleDataService.getSampleData(location);
  }

  private void expectSampleDataException(String failureCode) {
    expectedException.expect(SampleDataException.class);
    expectedException.expect(exceptionMatcher(failureCode));
  }
}
