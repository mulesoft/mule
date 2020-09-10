/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.data.sample.SampleDataTestUtils.exceptionMatcher;
import static org.mule.sdk.api.data.sample.SampleDataException.INVALID_LOCATION;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import static org.mule.sdk.api.data.sample.SampleDataException.NO_DATA_AVAILABLE;
import static org.mule.sdk.api.data.sample.SampleDataException.UNKNOWN;
import static org.mule.test.allure.AllureConstants.SampleData.SAMPLE_DATA;
import static org.mule.test.allure.AllureConstants.SampleData.SampleDataStory.SAMPLE_DATA_SERVICE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.data.sample.ComponentSampleDataProvider;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(SAMPLE_DATA)
@Story(SAMPLE_DATA_SERVICE)
public class MuleSampleDataServiceTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private Location location;

  @Mock
  private ConfigurationComponentLocator componentLocator;

  @Mock(extraInterfaces = {Component.class})
  private ComponentSampleDataProvider componentSampleDataProvider;

  @Mock
  private Message message;

  private MuleSampleDataService sampleDataService = new MuleSampleDataService();

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
