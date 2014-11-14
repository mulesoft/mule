/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.response.HttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Object that sends an HTTP request, and returns the response.
 */
public interface HttpClient extends Initialisable, Stoppable
{

    public HttpResponse send(HttpRequest request, int responseTimeout, boolean followRedirects, HttpAuthentication authentication) throws IOException, TimeoutException;

}
