/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.http2.api.client.Http2ClientFactory;
import org.mule.runtime.http2.api.server.Http2ServerFactory;
import org.mule.runtime.http2.api.server.Http2Server;
import org.mule.runtime.http2.api.client.Http2Client;

/**
 * Provides HTTP/2 server and client factories.
 *
 * @since 4.5
 */
@NoImplement
public interface Http2Service extends Service {

  /**
   * @return an {@link Http2ServerFactory} capable of creating {@link Http2Server}s.
   */
  Http2ServerFactory getServerFactory();

  /**
   * @return an {@link Http2ClientFactory} capable of creating {@link Http2Client}s.
   */
  Http2ClientFactory getClientFactory();
}
