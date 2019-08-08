/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
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
   * Tests if {@code this} socket supports reconnection by the means of the {@link #reconnect(RetryPolicyTemplate, Scheduler)}
   * method.
   *
   * @return Whether reconnection is supported for {@code this} instance.
   * @since 4.2.2
   */
  boolean supportsReconnection();

  /**
   * Reconnects by opening a new {@link WebSocket} generated from an HTTP request identical to the one that originally spawned
   * {@code this} one. The new WebSocket will have <b>THE SAME</b> {@link #getId()} as the original instance.
   * <p>
   * The term reconnection is not to be interpreted here as in the &quot;socket MUST have had a connectivity issue first&quot;.
   * Although recovering from connectivity issues is one of the prime uses of this method, that is not a prerequisite. Invoking
   * this method on a perfectly functioning instance will just spawn a new WebSocket connected to the same remote system. However,
   * be mindful that the new WebSocket will share the same id as this one. Having two active sockets with the same ID might be
   * problematic depending on the use case.
   * <p>
   * This method does not alter the current state of {@code this} instance. It merely generates a new WebSocket similar to
   * this one.
   * <p>
   * Not all implementations are required to support this method as it's not possible to do in some cases. The general contract
   * is that this method should only be called when {@link #supportsReconnection()} returns {@code true}. Invoking this method
   * on an implementation that doesn't support it will result in a {@link CompletableFuture} immediately and exceptionally
   * completed with an {@link javax.naming.OperationNotSupportedException}.
   *
   * @param retryPolicyTemplate the retry policy to use while reconnecting
   * @param scheduler           the scheduler on which reconnection work should happen
   * @return a {@link CompletableFuture} with the newly generated {@link WebSocket}
   * @since 4.2.2
   */
  CompletableFuture<WebSocket> reconnect(RetryPolicyTemplate retryPolicyTemplate, Scheduler scheduler);

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

  /**
   * Tests whether the {@link #close(WebSocketCloseCode, String)} method has been called on {@code this} instance or not.
   * <p>
   * Notice that this method differs from {@link #isConnected()} in that even though the socket might not have been closed,
   * it might still have lost its connection to the remote system. There's no forced correlation between the output values of
   * both methods.
   *
   * @return Whether this socket has been closed or not
   * @since 4.2.2
   */
  boolean isClosed();

  /**
   * Tests whether this socket's connection to the remote system is still active.
   * <p>
   * Notice that this method differs from {@link #isConnected()} in that even though the socket might not have been closed,
   * it might still have lost its connection to the remote system. There's no forced correlation between the output values of
   * both methods.
   *
   * @return Whether this socket's connection is still active.
   * @since 4.2.2
   */
  boolean isConnected();
}
