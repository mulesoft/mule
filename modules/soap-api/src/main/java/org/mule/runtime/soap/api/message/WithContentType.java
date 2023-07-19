/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.soap.api.message;

import org.mule.runtime.api.metadata.MediaType;

/**
 * Contract for Objects that carries a content-type.
 *
 * @since 4.0
 */
public interface WithContentType {

  /**
   * @return the content type of the attachment content.
   */
  MediaType getContentType();
}
