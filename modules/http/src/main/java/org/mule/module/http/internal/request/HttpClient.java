/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.api.CompletionHandler;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.domain.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Object that sends an HTTP request, and returns the response.
 */
public interface HttpClient extends Initialisable, Stoppable
{

    /**
     * Sends a HttpRequest blocking the current thread until a response is available for the request times out.
     */
    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication) throws IOException, TimeoutException;

    /**
     * Sends a HttpRequest without blocking the current thread.  When a response is available or the request times out the provided CompletionHandler will be invoked.
     */
    public void send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpRequestAuthentication authentication, final CompletionHandler<HttpResponse, Exception> handler, WorkManager responseWorkManager);

}
