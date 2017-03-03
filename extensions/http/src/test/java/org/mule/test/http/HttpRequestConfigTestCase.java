/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.http;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.internal.connection.ConnectionProviderWrapper.unwrapProviderWrapper;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.service.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;

import org.mule.extension.http.internal.request.HttpRequesterProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import org.junit.Test;

public class HttpRequestConfigTestCase extends AbstractHttpTestCase {

  private static final String DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME = "requestConfigHttp";
  private static final String DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME = "requestConfigHttps";

  @Override
  protected String getConfigFile() {
    return "http-request-functional-config.xml";
  }

  @Test
  public void requestConfigDefaultPortHttp() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTP_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getPort(), is(HTTP.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultPortHttps() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getPort(), is(HTTPS.getDefaultPort()));
  }

  @Test
  public void requestConfigDefaultTlsContextHttps() throws Exception {
    ConfigurationInstance config =
        getConfigurationInstanceFromRegistry(DEFAULT_PORT_HTTPS_REQUEST_CONFIG_NAME, testEvent(), muleContext);
    HttpRequesterProvider provider = (HttpRequesterProvider) unwrapProviderWrapper(config.getConnectionProvider().get());
    assertThat(provider.getTlsContext(), notNullValue());
  }

}
