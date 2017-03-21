/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message;

/**
 * Represents and enables the construction of a SOAP header.
 *
 * @since 4.0
 */
public interface SoapHeader {

  /**
   * @return the id of the header.
   */
  String getId();

  /**
   * @return the content of the header.
   */
  String getValue();

}
