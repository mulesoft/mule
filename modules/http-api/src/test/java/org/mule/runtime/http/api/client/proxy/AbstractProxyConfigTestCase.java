/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
