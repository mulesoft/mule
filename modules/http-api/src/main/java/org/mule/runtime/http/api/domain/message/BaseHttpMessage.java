/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message;

import static org.mule.runtime.api.util.MultiMap.emptyMultiMap;
import org.mule.runtime.api.util.MultiMap;

/**
 * Base implementation for {@HttpMessage} that handles ignore case header operations. The lower case version is attempted first.
 *
 * @since 3.9
 */
public abstract class BaseHttpMessage extends BaseMessageWithHeaders implements HttpMessage {

  public BaseHttpMessage() {
    this(emptyMultiMap());
  }

  public BaseHttpMessage(MultiMap<String, String> headers) {
    super(headers);
  }
}
