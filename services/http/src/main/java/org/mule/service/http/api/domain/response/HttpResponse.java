/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.response;

import org.mule.service.http.api.domain.HttpMessage;

public interface HttpResponse extends HttpMessage {

  int getStatusCode();

  void setStatusCode(int statusCode);

  String getReasonPhrase();

  void setReasonPhrase(String reasonPhrase);

}
