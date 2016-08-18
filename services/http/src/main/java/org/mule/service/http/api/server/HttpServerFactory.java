/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.server;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * Factory object for {@link HttpServer}
 *
 * @since 4.0
 */
public interface HttpServerFactory {

  HttpServer create(HttpServerConfiguration serverConfiguration) throws ConnectionException;
}
