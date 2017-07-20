/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientConfiguration;
import org.mule.runtime.soap.api.client.SoapClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ForwardingSoapClientConnectionProviderTestCase {

  private static final String ERROR_NAME = "AN_ERROR";
  private static final String ERROR_NS = "NS";
  private static final String ERROR_MESSAGE = "ERROR MESSAGE";
  private static final Exception EXCEPTION = new RuntimeException(ERROR_MESSAGE);
  private static final ErrorType ERROR_TYPE = mock(ErrorType.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MessageDispatcherProvider dispatcherProvider = mock(MessageDispatcherProvider.class);
  private MuleContext muleContext = mock(MuleContext.class);

  @Before
  public void setup() {
    when(ERROR_TYPE.getNamespace()).thenReturn(ERROR_NS);
    when(ERROR_TYPE.getIdentifier()).thenReturn(ERROR_NAME);
  }

  @Test
  public void invalidProvider() throws ConnectionException {
    expectedException.expectMessage(ERROR_MESSAGE);
    expectedException.expect(ConnectionException.class);
    expectedException.expectCause(instanceOf(RuntimeException.class));
    ForwardingSoapClientConnectionProvider provider =
      new ForwardingSoapClientConnectionProvider(new ValidableServiceProvider(false), dispatcherProvider, muleContext);
    provider.connect();
  }

  @Test
  public void validProvider() throws ConnectionException {
    ForwardingSoapClientConnectionProvider provider =
      new ForwardingSoapClientConnectionProvider(new ValidableServiceProvider(true), dispatcherProvider, muleContext);
    ForwardingSoapClient client = provider.connect();
    assertThat(client.getAllWebServices(), hasSize(1));
  }

  private class ValidableServiceProvider implements SoapServiceProvider {

    private final boolean valid;

    ValidableServiceProvider(boolean valid) {
      this.valid = valid;
    }

    @Override
    public List<WebServiceDefinition> getWebServiceDefinitions() {
      try {
        return asList(WebServiceDefinition.builder()
                        .withId("dos")
                        .withFriendlyName("Another Service Name")
                        .withWsdlUrl(new URL("http://localhost.com/dos"))
                        .withService("Service")
                        .withPort("Port2").build());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ConnectionValidationResult validate() {
      return valid ? success() : failure(ERROR_MESSAGE, ERROR_TYPE, EXCEPTION);
    }
  }
}

