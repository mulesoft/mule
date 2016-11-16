/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;


import static java.lang.String.format;
import static javax.xml.ws.Endpoint.publish;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.ws.consumer.SimpleService;

import javax.xml.ws.Endpoint;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.rules.ExternalResource;

public class WebServiceRule extends ExternalResource {

  private static final String OPERATIONS_URL_MASK = "http://localhost:%s/test";

  private String address;
  private Endpoint service;

  public WebServiceRule(String port) {
    this.address = format(OPERATIONS_URL_MASK, port);
  }

  @Override
  protected void before() throws Throwable {
    XMLUnit.setIgnoreWhitespace(true);
    service = publish(address, new SimpleService());
    assertThat(service.isPublished(), is(true));
  }

  @Override
  protected void after() {
    if (service != null) {
      service.stop();
    }
  }

  public String getAddress() {
    return address;
  }
}
