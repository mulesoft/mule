/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.mockito.Mockito.mock;
import org.mule.service.http.api.domain.message.response.HttpResponse;

public class ResponseCompletionHandlerTestCase extends BaseResponseCompletionHandlerTestCase {

  private ResponseCompletionHandler handler = new ResponseCompletionHandler(ctx, request, mock(HttpResponse.class), callback);

  @Override
  protected BaseResponseCompletionHandler getHandler() {
    return handler;
  }

}
