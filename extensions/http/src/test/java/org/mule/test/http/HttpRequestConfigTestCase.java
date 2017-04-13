/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.http;

import static org.mule.extension.http.internal.request.RequestConnectionParams.DEFAULT_CONNECTION_IDLE_TIMEOUT;
import static org.mule.extension.http.internal.request.RequestConnectionParams.DEFAULT_MAX_CONNECTIONS;
import static org.mule.extension.http.internal.request.RequestConnectionParams.DEFAULT_RESPONSE_BUFFER_SIZE;
import static org.mule.runtime.core.internal.connection.ConnectionProviderWrapper.unwrapProviderWrapper;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.internal.request.HttpRequesterProvider;
import org.mule.extension.http.internal.request.RequestConnectionParams;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestConfigTestCase extends AbstractHttpTestCase {

  private static final String DEFAULT_HTTP_REQUEST_CONFIG_NAME = "requestConfig";
  private static final String DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME = "requestConfigHttp";
  private static final String DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME = "requestConfigHttps";
  private static final int RESPONSE_BUFFER_SIZE = 1024;
  private static final int IDLE_TIMEOUT = 10000;
  private static final int MAX_CONNECTIONS = 1;

  @Rule
  public SystemProperty bufferSize = new SystemProperty("bufferSize", String.valueOf(RESPONSE_BUFFER_SIZE));
  @Rule
  public SystemProperty maxConnections = new SystemProperty("maxConnections", String.valueOf(MAX_CONNECTIONS));
  @Rule
  public SystemProperty idleTimeout = new SystemProperty("idleTimeout", String.valueOf(IDLE_TIMEOUT));

  @Override
  protected String getConfigFile() {
    return "http-request-functional-config.xml";
  }

  @Test
  public void requestConfigDefaultPortHttp() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getConnectionParams().getPort(), is(HTTP.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultPortHttps() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getConnectionParams().getPort(), is(HTTPS.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultTlsContextHttps() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getTlsContext(), notNullValue());
  }

  @Test
  public void requestConfigDefaults() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_HTTP_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    RequestConnectionParams connectionParams = provider.getConnectionParams();
    assertThat(connectionParams.getResponseBufferSize(), is(Integer.valueOf(DEFAULT_RESPONSE_BUFFER_SIZE)));
    assertThat(connectionParams.getMaxConnections(), is(Integer.valueOf(DEFAULT_MAX_CONNECTIONS)));
    assertThat(connectionParams.getConnectionIdleTimeout(), is(Integer.valueOf(DEFAULT_CONNECTION_IDLE_TIMEOUT)));
  }

  @Test
  public void requestConfigOverrideDefaults() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    RequestConnectionParams connectionParams = provider.getConnectionParams();
    assertThat(connectionParams.getResponseBufferSize(), is(RESPONSE_BUFFER_SIZE));
    assertThat(connectionParams.getMaxConnections(), is(MAX_CONNECTIONS));
    assertThat(connectionParams.getConnectionIdleTimeout(), is(IDLE_TIMEOUT));
  }

}
