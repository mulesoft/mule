/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.sse;

import org.mule.runtime.http.api.domain.sse.ServerSentEvent;

public interface ServerSentEventListener {

  default void onOpen() {}

  default void onClose() {}

  default void onError(Throwable error) {}

  default void onEvent(ServerSentEvent event) {}
}
