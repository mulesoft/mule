/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class AbstractProxyConfigTestCase<B extends BaseProxyConfigBuilder> {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected static final String HOST = "host";
  protected static final int PORT = 8080;
  protected static final String USERNAME = "username";
  protected static final String PASSWORD = "password";
  protected static final String NON_PROXY_HOSTS = "host1,host2";


  @Test
  public void onlyHost() {
    expectedException.expect(IllegalArgumentException.class);
    getProxyConfigBuilder().host(HOST).build();
  }

  @Test
  public void onlyPort() {
    expectedException.expect(IllegalArgumentException.class);
    getProxyConfigBuilder().port(PORT).build();
  }

  protected abstract B getProxyConfigBuilder();
}
