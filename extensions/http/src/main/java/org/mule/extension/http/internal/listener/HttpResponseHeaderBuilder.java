/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static com.google.common.collect.Multimaps.newMultimap;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;

public class HttpResponseHeaderBuilder {

  private List<String> uniqueHeadersNames =
      asList(TRANSFER_ENCODING.toLowerCase(), CONTENT_LENGTH.toLowerCase(), CONTENT_TYPE.toLowerCase());

  Multimap<String, String> headers = newMultimap(new CaseInsensitiveMapWrapper<>(), Sets::newHashSet);

  public void addHeader(String headerName, Object headerValue) {
    if (headerValue instanceof Iterable) {
      failIfHeaderDoesNotSupportMultipleValues(headerName);
      Iterable values = (Iterable) headerValue;
      for (Object value : values) {
        addSimpleValue(headerName, value.toString());
      }
    } else if (headerValue instanceof String[]) {
      failIfHeaderDoesNotSupportMultipleValues(headerName);
      String[] values = (String[]) headerValue;
      for (String value : values) {
        addSimpleValue(headerName, value);
      }
    } else {
      addSimpleValue(headerName, headerValue.toString());
    }
  }

  public Collection<String> removeHeader(String headerName) {
    return headers.removeAll(headerName);
  }

  private void failIfHeaderDoesNotSupportMultipleValues(String headerName) {
    if (uniqueHeadersNames.contains(headerName.toLowerCase())) {
      throw new MuleRuntimeException(createStaticMessage("Header " + headerName + " does not support multiple values"));
    }
  }

  private void addSimpleValue(String headerName, String headerValue) {
    if (headers.containsKey(headerName)) {
      failIfHeaderDoesNotSupportMultipleValues(headerName);
    }
    headers.put(headerName, headerValue);
  }

  public String getContentType() {
    return getSimpleValue(CONTENT_TYPE);
  }

  public String getTransferEncoding() {
    return getSimpleValue(TRANSFER_ENCODING);
  }

  public String getContentLength() {
    return getSimpleValue(CONTENT_LENGTH);
  }

  private String getSimpleValue(String header) {
    if (!headers.containsKey(header)) {
      return null;
    }
    return (String) ((Collection) headers.get(header)).iterator().next();
  }

  public void addContentType(String multipartFormData) {
    addSimpleValue(CONTENT_TYPE, multipartFormData);
  }

  public void setContentLenght(String calculatedContentLenght) {
    removeHeader(CONTENT_LENGTH);
    addSimpleValue(CONTENT_LENGTH, calculatedContentLenght);
  }

  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  public Collection<String> getHeader(String headerName) {
    return headers.get(headerName);
  }
}
