/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;

import java.util.concurrent.CompletableFuture;

public interface ReconnectableWebSocket extends WebSocket {

  CompletableFuture<WebSocket> reconnect(RetryPolicyTemplate retryPolicyTemplate, Scheduler scheduler);
}
