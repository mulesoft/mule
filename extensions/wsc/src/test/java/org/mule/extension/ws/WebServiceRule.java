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

import javax.xml.ws.Endpoint;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.rules.ExternalResource;

public class WebServiceRule extends ExternalResource {

  private static final String OPERATIONS_URL_MASK = "http://localhost:%s";

  private String address;
  private Endpoint service;
  private Object serviceImpl;

  public WebServiceRule(String port, String path, Object serviceImpl) {
    path = !path.startsWith("/") ? "/" + path : path;
    this.address = format(OPERATIONS_URL_MASK + path, port);
    this.serviceImpl = serviceImpl;
  }

  @Override
  protected void before() throws Throwable {
    XMLUnit.setIgnoreWhitespace(true);
    service = publish(address, serviceImpl);
    assertThat(service.isPublished(), is(true));
  }

  @Override
  protected void after() {
    stopService(service);
  }

  private void stopService(Endpoint service) {
    if (service != null) {
      service.stop();
    }
  }

  public String get11Address() {
    return address;
  }
}
