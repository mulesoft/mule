/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.issues;

import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;


public class Mule5415TestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/mule-5415-config.xml";
  }

  @Test
  public void testFirstRequestDoesntFail() throws Exception {
    MuleClient client = muleContext.getClient();
    client.send(String.format("http://localhost:%s?param1=1&param2=3", port1.getNumber()),
                InternalMessage.builder().payload("message").mediaType(APPLICATION_X_WWW_FORM_URLENCODED).build(),
                newOptions().method(POST.name()).build())
        .getRight();
  }
}
