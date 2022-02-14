/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message.content;

import java.io.InputStream;

public interface Http2Content {

  /**
   * @return the content as an {@link InputStream}.
   */
  InputStream asInputStream();

  boolean isEmpty();
}
