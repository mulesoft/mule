/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api;

import org.mule.runtime.api.service.Service;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.server.HttpServerFactory;

public interface HttpService extends Service {

  HttpServerFactory getServerFactory();

  HttpClientFactory getClientFactory();

}
