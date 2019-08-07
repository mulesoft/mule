/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.server.HttpServer;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Mule's abstraction of a Web Socket.
 * <p>
 * Each socket is classified according to a {@link WebSocketType type} and identified by an unique {@link #getId() id}
 * <p>
 * Sockets can also be labeled with groups, which allow to logically gather sets of sockets by a random taxonomy.
 *
 * @since 4.2.0
 */
@NoImplement
public interface WebSocket {

  /**
   * Types of WebSocket
   *
   * @since 4.2.0
   */
  enum WebSocketType {

    /**
     * Type for sockets that were opened by an external request received through an {@link HttpServer}
     */
    INBOUND,

    /**
     * Type for sockets that were opened by a request locally originated through a {@link HttpClient}
     */
    OUTBOUND
  }

  /**
   * @return Unique identified for {@code this} socket
   */
  String getId();

  /**
   * @return {@code this} socket's type
   */
  WebSocketType getType();

  /**
   * @return {@code this} socket's protocol
   */
  WebSocketProtocol getProtocol();

  /**
   * @return The {@link URI} of the endpoint that received the originating HTTP request.
   */
  URI getUri();

  /**
   * @return an immutable list with the groups to which {@code this} socket belongs to. Maybe empty but never null.
   */
  List<String> getGroups();

  /**
   * Subscribes {@code this} socket to the given {@code group}
   *
   * @param group a group. Not null nor empty.
   */
  void addGroup(String group);

  /**
   * Unsubscribe {@code this} socket to the given {@code group}
   *
   * @param group a group. Not null nor empty.
   */
  void removeGroup(String group);

  /**
   * Sends the given {@code content} to the remote peer.
   *
   * @param content   the content to be sent
   * @param mediaType the content's {@link MediaType}
   * @return a {@link CompletableFuture}
   */
  CompletableFuture<Void> send(InputStream content, MediaType mediaType);

  /**
   * Closes {@code this} socket
   *
   * @param code   {@link WebSocketCloseCode} the close code
   * @param reason the reason why it's being closed
   * @return a {@link CompletableFuture}
   */
  CompletableFuture<Void> close(WebSocketCloseCode code, String reason);

  boolean isClosed();
}
