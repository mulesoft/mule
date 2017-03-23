/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.util;


/**
 * {@link Exception} implementation that aims to be thrown when an XML transformation problem occur.
 *
 * @since 4.0
 */
public class XmlTransformationException extends Exception {

  XmlTransformationException(String message, Throwable cause) {
    super(message, cause);
  }
}
