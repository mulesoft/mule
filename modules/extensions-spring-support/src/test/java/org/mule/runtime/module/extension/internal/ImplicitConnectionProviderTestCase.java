/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ImplicitConnectionProviderTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {ImplicitConnectionProviderExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "implicit-connection-provider.xml";
  }

  @Test
  public void getImplicitConnection() throws Exception {
    Counter connection =
        (Counter) flowRunner("implicitConnection").withVariable("number", 5).run().getMessage().getPayload().getValue();
    assertThat(connection.getValue(), is(5));

    connection = (Counter) flowRunner("implicitConnection").withVariable("number", 10).run().getMessage().getPayload().getValue();
    assertThat(connection.getValue(), is(10));
  }

  @Extension(name = "implicit")
  @Operations({ImplicitOperations.class})
  @Xml(namespace = "http://www.mulesoft.org/schema/mule/implicit", prefix = "implicit")
  @ConnectionProviders(ImplicitConnectionProvider.class)
  public static class ImplicitConnectionProviderExtension {

  }

  public static class ImplicitConnectionProvider implements ConnectionProvider<Counter> {

    @Parameter
    @Optional(defaultValue = "#[number]")
    private int number;

    @Override
    public Counter connect() throws ConnectionException {
      return new Counter(number);
    }

    @Override
    public void disconnect(Counter counter) {

    }

    @Override
    public ConnectionValidationResult validate(Counter counter) {
      return ConnectionValidationResult.success();
    }
  }

  public static class ImplicitOperations {

    public Counter getConnection(@Connection Counter connection) {
      return connection;
    }
  }

  public static class Counter {

    private AtomicInteger value;

    public Counter(int value) {
      this.value = new AtomicInteger(value);
    }

    public int incrementAndGet() {
      return value.incrementAndGet();
    }

    public int getValue() {
      return value.get();
    }
  }

}
