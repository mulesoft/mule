/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.multipart;

import javax.mail.internet.MimeMultipart;

/**
 *
 */
public class HttpMimeMultipart extends MimeMultipart {

  public HttpMimeMultipart(String contentType, String subtype) {
    super(subtype);
    this.contentType = contentType;
  }
}
