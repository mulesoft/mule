/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.entity.multipart;

import org.mule.service.http.api.domain.entity.HttpEntity;

import java.util.Collection;

public class MultipartHttpEntity implements HttpEntity {

  private final Collection<HttpPart> parts;

  public MultipartHttpEntity(final Collection<HttpPart> parts) {
    this.parts = parts;
  }

  public Collection<HttpPart> getParts() {
    return this.parts;
  }

}
