/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConnectionProviderNameModelValidatorTestCase extends AbstractMuleTestCase {

  private static final String CUSTOM_NAME = "my name";
  private static final String EXTENSION_NAME = "my extension";

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
  }

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  private ConnectionProviderNameModelValidator validator = new ConnectionProviderNameModelValidator();

  private List<ConnectionProviderModel> mockConnectionProviders(String... names) {
    return Stream.of(names).map(name -> {
      ConnectionProviderModel mock = mock(ConnectionProviderModel.class, Mockito.RETURNS_DEEP_STUBS);
      when(mock.getName()).thenReturn(name);

      return mock;
    }).collect(toList());
  }

  @Test
  public void valid() {
    List<ConnectionProviderModel> mockModels = mockConnectionProviders(DEFAULT_CONNECTION_PROVIDER_NAME, CUSTOM_NAME);
    when(extensionModel.getConnectionProviders()).thenReturn(mockModels);
    validator.validate(extensionModel);
  }

  @Test
  public void invalid() {
    List<ConnectionProviderModel> mockModels =
        mockConnectionProviders(DEFAULT_CONNECTION_PROVIDER_NAME, DEFAULT_CONNECTION_PROVIDER_NAME, CUSTOM_NAME, CUSTOM_NAME,
                                "bleh");
    when(extensionModel.getConnectionProviders()).thenReturn(mockModels);
    expectedException.expect(new BaseMatcher<Exception>() {

      @Override
      public boolean matches(Object item) {
        assertThat(item, instanceOf(IllegalModelDefinitionException.class));
        Exception e = (Exception) item;

        assertThat(e.getMessage(), containsString(EXTENSION_NAME));
        assertThat(e.getMessage(), containsString(DEFAULT_CONNECTION_PROVIDER_NAME));
        assertThat(e.getMessage(), containsString(CUSTOM_NAME));
        assertThat(e.getMessage(), containsString("4"));

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("conditions not met");
      }
    });

    validator.validate(extensionModel);
  }

  @Test
  public void noConnectionProviders() {
    when(extensionModel.getConnectionProviders()).thenReturn(emptyList());
    validator.validate(extensionModel);
  }
}
