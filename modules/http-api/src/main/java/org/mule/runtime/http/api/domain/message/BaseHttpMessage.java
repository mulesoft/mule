/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message;

import static org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap.emptyCaseInsensitiveMultiMap;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;

/**
 * Base implementation for {@HttpMessage} that handles ignore case header operations. The lower case version is attempted first.
 *
 * @since 3.9
 */
public abstract class BaseHttpMessage extends BaseMessageWithHeaders implements HttpMessage {

  public BaseHttpMessage() {
    super(emptyCaseInsensitiveMultiMap());
  }

  /**
   * @deprecated since 2.0.0. Use {@link #BaseHttpMessage(CaseInsensitiveMultiMap)} instead
   */
  @Deprecated
  public BaseHttpMessage(MultiMap<String, String> headers) {
    super(headers);
  }

  public BaseHttpMessage(CaseInsensitiveMultiMap headers) {
    super(headers);
  }
}
