/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
