/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.internal.loader.validator.ConnectionProviderNameModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.stream.Stream;

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
    validate(extensionModel, validator);
  }

  @Test
  public void invalid() {
    List<ConnectionProviderModel> mockModels =
        mockConnectionProviders(DEFAULT_CONNECTION_PROVIDER_NAME, DEFAULT_CONNECTION_PROVIDER_NAME, CUSTOM_NAME, CUSTOM_NAME,
                                "bleh");
    when(extensionModel.getConnectionProviders()).thenReturn(mockModels);
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(containsString("4"));

    validate(extensionModel, validator);
  }

  @Test
  public void noConnectionProviders() {
    when(extensionModel.getConnectionProviders()).thenReturn(emptyList());
    validate(extensionModel, validator);
  }
}
