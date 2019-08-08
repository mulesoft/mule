/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Broadcasts a given content to a group of supplied {@link WebSocket sockets}.
 * <p>
 * Instances are not to be assumed reusable.
 *
 * @since 4.2.0
 */
@NoImplement
public interface WebSocketBroadcaster {

  /**
   * Broadcast the {@code content} to the given {@code sockets}.
   * <p>
   * An {@code errorCallback} is used to notify errors while broadcasting. If is communication with
   * socket N fails, communication with all the remaining N + M sockets will still be attempted. The callback
   * will be invoked once per failing socket.
   * <p>
   * Sockets that are either closed or have lost connection will be discarded.
   *
   * @param sockets       the {@link WebSocket sockets} to broadcast to
   * @param content       the content to be sent
   * @param errorCallback an error notification callback. It will be invoked once per failing socket
   * @return a {@link CompletableFuture} to be completed when the message has been broadcast to all {@code sockets}
   */
  CompletableFuture<Void> broadcast(Collection<WebSocket> sockets,
                                    TypedValue<InputStream> content,
                                    BiConsumer<WebSocket, Throwable> errorCallback);

  /**
   * Broadcast the {@code content} to the given {@code sockets}.
   * <p>
   * An {@code errorCallback} is used to notify errors while broadcasting. If is communication with
   * socket N fails, communication with all the remaining N + M sockets will still be attempted. The callback
   * will be invoked once per failing socket.
   * <p>
   * Sockets that are either closed or have lost connection will be discarded.
   * <p>
   * Socket that have lost connection to the remote system and support reconnection will be reconnected using the
   * given {@code retryPolicyTemplate} and {@code reconnectionScheduler}
   *
   * @param sockets       the {@link WebSocket sockets} to broadcast to
   * @param content       the content to be sent
   * @param errorCallback an error notification callback. It will be invoked once per failing socket
   * @return a {@link CompletableFuture} to be completed when the message has been broadcast to all {@code sockets}
   * @since 4.2.2
   */
  CompletableFuture<Void> broadcast(Collection<WebSocket> sockets,
                                    TypedValue<InputStream> content,
                                    BiConsumer<WebSocket, Throwable> errorCallback,
                                    RetryPolicyTemplate retryPolicyTemplate,
                                    Scheduler reconnectionScheduler);
}
