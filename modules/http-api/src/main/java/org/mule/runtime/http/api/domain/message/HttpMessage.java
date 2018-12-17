/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message;

import org.mule.runtime.http.api.domain.entity.EmptyHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;

/**
 * Represents common parts of an HTTP message.
 *
 * @since 4.0
 */
public interface HttpMessage extends MessageWithHeaders {

  /**
   * @return the entity of the message. If there's no entity an {@link EmptyHttpEntity} is returned
   */
  HttpEntity getEntity();

}
