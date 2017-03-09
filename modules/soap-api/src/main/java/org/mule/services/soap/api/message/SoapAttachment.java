/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message;

import java.io.InputStream;

/**
 * Represents and enables the construction of an attachment to be sent over SOAP.
 *
 * @since 4.0
 */
public interface SoapAttachment extends WithContentType {

  /**
   * @return the content id of the attachment.
   */
  String getId();

  /**
   * @return the content of the attachment.
   */
  InputStream getContent();

}
