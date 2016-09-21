/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-10531")
public class HttpRequestApiConfigurationTestCase extends AbstractHttpTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-api-config.xml";
  }

  @Test
  public void parseApiConfigurationFromConfig() throws Exception {
    HttpRequesterConfig config = getConfigurationFromRegistry("ramlConfig", testEvent, muleContext);

    assertNotNull(config.getApiConfiguration());
    assertThat(config.getApiConfiguration().getLocation(), equalTo("TestFile.raml"));
  }
}
