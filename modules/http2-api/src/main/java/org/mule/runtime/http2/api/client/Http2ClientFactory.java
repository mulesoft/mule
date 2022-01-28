/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.client;

import org.mule.api.annotation.NoImplement;

/**
 * Factory object for {@link Http2Client}.
 *
 * @since 4.5
 */
@NoImplement
public interface Http2ClientFactory {

  /**
   * @param configuration the {@link Http2ClientConfiguration} specifying the desired client.
   * @return a newly built {@link Http2Client} based on the {@code configuration}.
   */
  Http2Client create(Http2ClientConfiguration configuration);
}
