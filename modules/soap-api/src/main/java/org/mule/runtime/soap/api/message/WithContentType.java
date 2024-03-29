/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
