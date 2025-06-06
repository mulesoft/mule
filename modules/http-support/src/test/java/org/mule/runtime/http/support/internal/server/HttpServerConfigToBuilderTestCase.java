/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_FORWARD_COMPATIBILITY;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.http.api.server.HttpServerConfiguration;

import java.util.function.Supplier;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Feature(HTTP_FORWARD_COMPATIBILITY)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpServerConfigToBuilderTestCase {

  @Mock
  private HttpServerConfiguration.Builder mockBuilder;

  @Mock
  private TlsContextFactory tlsContextFactory;

  @Mock
  private Supplier<Scheduler> schedulerSupplier;

  private HttpServerConfigToBuilder configurer;

  @BeforeEach
  void setUp() {
    when(mockBuilder.setHost(anyString())).thenReturn(mockBuilder);
    when(mockBuilder.setPort(anyInt())).thenReturn(mockBuilder);
    when(mockBuilder.setTlsContextFactory(any())).thenReturn(mockBuilder);
    when(mockBuilder.setUsePersistentConnections(anyBoolean())).thenReturn(mockBuilder);
    when(mockBuilder.setConnectionIdleTimeout(anyInt())).thenReturn(mockBuilder);
    when(mockBuilder.setSchedulerSupplier(any())).thenReturn(mockBuilder);
    when(mockBuilder.setName(anyString())).thenReturn(mockBuilder);
    when(mockBuilder.setReadTimeout(anyLong())).thenReturn(mockBuilder);

    configurer = new HttpServerConfigToBuilder(mockBuilder);
  }

  @Test
  void setHost() {
    configurer.setHost("localhost");
    verify(mockBuilder).setHost("localhost");
  }

  @Test
  void setPort() {
    configurer.setPort(8080);
    verify(mockBuilder).setPort(8080);
  }

  @Test
  void setTlsContextFactory() {
    configurer.setTlsContextFactory(tlsContextFactory);
    verify(mockBuilder).setTlsContextFactory(tlsContextFactory);
  }

  @Test
  void setUsePersistentConnections() {
    configurer.setUsePersistentConnections(true);
    verify(mockBuilder).setUsePersistentConnections(true);
  }

  @Test
  void setConnectionIdleTimeout() {
    configurer.setConnectionIdleTimeout(30000);
    verify(mockBuilder).setConnectionIdleTimeout(30000);
  }

  @Test
  void setSchedulerSupplier() {
    configurer.setSchedulerSupplier(schedulerSupplier);
    verify(mockBuilder).setSchedulerSupplier(schedulerSupplier);
  }

  @Test
  void setName() {
    configurer.setName("test-server");
    verify(mockBuilder).setName("test-server");
  }

  @Test
  void setReadTimeout() {
    configurer.setReadTimeout(5000L);
    verify(mockBuilder).setReadTimeout(5000L);
  }
}
