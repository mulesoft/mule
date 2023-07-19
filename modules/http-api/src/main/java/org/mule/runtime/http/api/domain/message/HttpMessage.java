/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
