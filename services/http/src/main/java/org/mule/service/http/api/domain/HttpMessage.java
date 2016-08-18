/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain;

import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;

import java.util.Collection;

/**
 * Represents common parts of an http message
 */
public interface HttpMessage {

  /**
   * @return all headers name
   */
  Collection<String> getHeaderNames();

  /**
   * @param headerName name of the header
   * @return first value of the header
   */
  String getHeaderValue(String headerName);

  /**
   * @param headerName name of the header
   * @return first value of the header, regardless of the case
   */
  String getHeaderValueIgnoreCase(String headerName);

  /**
   * @param headerName name of the header
   * @return all the values of that headers. If not such headers exists return null, otherwise the collection of header values
   */
  Collection<String> getHeaderValues(String headerName);

  /**
   * @param headerName name of the header
   * @return all the values of that headers, regardless of the case. If not such headers exists return null, otherwise the
   *         collection of header values
   */
  Collection<String> getHeaderValuesIgnoreCase(String headerName);

  /**
   * @return the entity of the message. If there's no entity an
   *         {@link EmptyHttpEntity} is returned
   */
  HttpEntity getEntity();

}
