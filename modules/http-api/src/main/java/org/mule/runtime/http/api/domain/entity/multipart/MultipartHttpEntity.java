/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity.multipart;

import org.mule.runtime.http.api.domain.entity.HttpEntity;

import java.io.InputStream;
import java.util.Collection;

/**
 * Represents a multipart HTTP body.
 *
 * @since 4.0
 */
public class MultipartHttpEntity implements HttpEntity {

  private final Collection<HttpPart> parts;

  public MultipartHttpEntity(final Collection<HttpPart> parts) {
    this.parts = parts;
  }

  @Override
  public boolean isStreaming() {
    return false;
  }

  @Override
  public boolean isComposed() {
    return true;
  }

  @Override
  public InputStream getContent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getBytes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<HttpPart> getParts() {
    return this.parts;
  }

}
