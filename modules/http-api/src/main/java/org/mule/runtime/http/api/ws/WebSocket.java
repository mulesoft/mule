/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.metadata.MediaType;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Experimental
public interface WebSocket {

  enum WebSocketType {

    INBOUND, OUTBOUND
  }

  String getId();

  WebSocketType getType();

  WebSocketProtocol getProtocol();

  URI getUri();

  List<String> getGroups();

  void addGroup(String group);

  void removeGroup(String group);

  CompletableFuture<Void> send(InputStream content, MediaType mediaType);

  CompletableFuture<Void> close(WebSocketCloseCode code, String reason);
}
