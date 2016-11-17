/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener.server;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;
import org.mule.runtime.module.http.internal.listener.HttpRequestParsingException;
import org.mule.runtime.module.http.internal.listener.async.RequestHandler;

public interface ExtensionRequestHandler extends RequestHandler {

  Result<Object, HttpRequestAttributes> createResult(HttpRequestContext requestContext) throws HttpRequestParsingException;

}
