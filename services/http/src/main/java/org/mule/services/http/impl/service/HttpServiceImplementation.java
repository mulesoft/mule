/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SCHEDULER_BASE_CONFIG;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.HttpServerFactory;
import org.mule.services.http.impl.service.client.GrizzlyHttpClient;
import org.mule.services.http.impl.service.server.HttpListenerConnectionManager;
import org.mule.services.http.impl.service.server.ContextHttpServerFactoryAdapter;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Implementation of {@link HttpService} that uses Grizzly to create {@link HttpServer}s and its Async HTTP Client provider to
 * create {@link HttpClient}s.
 */
public class HttpServiceImplementation implements HttpService, Startable, Stoppable {

  private static final Logger logger = getLogger(HttpServiceImplementation.class);
  private static final String CONTAINER_CONTEXT = "container";

  protected final SchedulerService schedulerService;

  private HttpListenerConnectionManager connectionManager;

  public HttpServiceImplementation(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
    connectionManager = new HttpListenerConnectionManager(schedulerService, config());
  }

  @Override
  public HttpServerFactory getServerFactory() {
    return new ContextHttpServerFactoryAdapter(CONTAINER_CONTEXT, connectionManager);
  }

  @Inject
  public HttpServerFactory getServerFactory(@Named(OBJECT_MULE_CONTEXT) MuleContext muleContext) {
    return new ContextHttpServerFactoryAdapter(muleContext.getId(), connectionManager);
  }

  @Override
  public HttpClientFactory getClientFactory() {
    return config -> new GrizzlyHttpClient(config, schedulerService, config());
  }

  @Inject
  public HttpClientFactory getClientFactory(@Named(OBJECT_SCHEDULER_BASE_CONFIG) SchedulerConfig schedulersConfig) {
    return config -> new GrizzlyHttpClient(config, schedulerService, schedulersConfig);
  }

  @Override
  public String getName() {
    return "HTTP Service";
  }

  @Override
  public void start() throws MuleException {
    initialiseIfNeeded(connectionManager);
  }

  @Override
  public void stop() throws MuleException {
    disposeIfNeeded(connectionManager, logger);
  }

}
