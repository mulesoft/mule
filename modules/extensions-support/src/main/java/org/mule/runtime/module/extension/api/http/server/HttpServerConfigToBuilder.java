/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.sdk.api.http.server.HttpServerConfig;

import java.util.Objects;
import java.util.function.Supplier;

public final class HttpServerConfigToBuilder implements HttpServerConfig {

  private final HttpServerConfiguration.Builder builder;

  public HttpServerConfigToBuilder(HttpServerConfiguration.Builder builder) {
    this.builder = builder;
  }

  @Override
  public HttpServerConfig setHost(String host) {
    builder.setHost(host);
    return this;
  }

  @Override
  public HttpServerConfig setPort(int port) {
    builder.setPort(port);
    return this;
  }

  @Override
  public HttpServerConfig setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    builder.setTlsContextFactory(tlsContextFactory);
    return this;
  }

  @Override
  public HttpServerConfig setUsePersistentConnections(boolean usePersistentConnections) {
    builder.setUsePersistentConnections(usePersistentConnections);
    return this;
  }

  @Override
  public HttpServerConfig setConnectionIdleTimeout(int connectionIdleTimeout) {
    builder.setConnectionIdleTimeout(connectionIdleTimeout);
    return this;
  }

  @Override
  public HttpServerConfig setSchedulerSupplier(Supplier<Scheduler> schedulerSupplier) {
    builder.setSchedulerSupplier(schedulerSupplier);
    return this;
  }

  @Override
  public HttpServerConfig setName(String name) {
    builder.setName(name);
    return this;
  }

  @Override
  public HttpServerConfig setReadTimeout(long readTimeout) {
    builder.setReadTimeout(readTimeout);
    return this;
  }

  public HttpServerConfiguration.Builder builder() {
    return builder;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    var that = (HttpServerConfigToBuilder) obj;
    return Objects.equals(this.builder, that.builder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(builder);
  }

  @Override
  public String toString() {
    return "HttpServerConfigurerToBuilder[" +
        "builder=" + builder + ']';
  }

}
