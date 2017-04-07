/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.internal.request.HttpRequesterConfig;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestApiConfigurationTestCase extends AbstractHttpTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-api-config.xml";
  }

  @Test
  public void parseApiConfigurationFromConfig() throws Exception {
    HttpRequesterConfig config = getConfigurationFromRegistry("ramlConfig", testEvent(), muleContext);

    assertNotNull(config.getApiConfiguration());
    assertThat(config.getApiConfiguration().getLocation(), equalTo("TestFile.raml"));
  }
}
