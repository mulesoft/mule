/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener.server;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.module.http.internal.listener.Server;

/**
 * Factory object for {@link Server}
 *
 * @since 4.0
 */
public interface HttpServerFactory
{
    Server create(HttpServerConfiguration serverConfiguration) throws ConnectionException;
}
