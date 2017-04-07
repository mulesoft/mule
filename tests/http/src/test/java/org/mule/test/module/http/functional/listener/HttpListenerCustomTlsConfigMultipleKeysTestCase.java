/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;


import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpListenerCustomTlsConfigMultipleKeysTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-listener-custom-tls-multiple-keys-config.xml";
  }

  @Test
  public void acceptsConnectionWithValidCertificate() throws Exception {
    Event event = flowRunner("testFlowClientWithCertificate").withPayload(TEST_MESSAGE).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Test(expected = MessagingException.class)
  public void rejectsConnectionWithInvalidCertificate() throws Exception {
    flowRunner("testFlowClientWithoutCertificate").withPayload(TEST_MESSAGE).run();
  }


}
