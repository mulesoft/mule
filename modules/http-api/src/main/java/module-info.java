/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Mule service that allows to create HTTP servers and clients.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.http.api {

  requires transitive org.mule.runtime.api;
  // for api.retry.policy and api.config.bootstrap
  requires org.mule.runtime.core;

  requires com.github.benmanes.caffeine;

  exports org.mule.runtime.http.api;
  exports org.mule.runtime.http.api.client;
  exports org.mule.runtime.http.api.client.auth;
  exports org.mule.runtime.http.api.client.proxy;
  exports org.mule.runtime.http.api.client.ws;
  exports org.mule.runtime.http.api.domain;
  exports org.mule.runtime.http.api.domain.entity;
  exports org.mule.runtime.http.api.domain.entity.multipart;
  exports org.mule.runtime.http.api.domain.message;
  exports org.mule.runtime.http.api.domain.message.request;
  exports org.mule.runtime.http.api.domain.message.response;
  exports org.mule.runtime.http.api.domain.request;
  exports org.mule.runtime.http.api.exception;
  exports org.mule.runtime.http.api.server;
  exports org.mule.runtime.http.api.server.async;
  exports org.mule.runtime.http.api.server.ws;
  exports org.mule.runtime.http.api.utils;
  exports org.mule.runtime.http.api.tcp;
  exports org.mule.runtime.http.api.ws;
  exports org.mule.runtime.http.api.ws.exception;
  
}
