/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.http.api.HttpConstants;

public class FailureStatusCodeValidator extends RangeStatusCodeValidator {

  @Override
  public void validate(Event responseEvent) throws ResponseValidatorException {
    int status = responseEvent.getMessage().getInboundProperty(HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY);

    if (belongs(status)) {
      throw new ResponseValidatorException(String.format("Response code %d mapped as failure", status));
    }
  }

}
