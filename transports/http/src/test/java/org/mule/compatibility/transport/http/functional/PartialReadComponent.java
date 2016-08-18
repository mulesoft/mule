/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Callable;

import java.io.InputStream;

public class PartialReadComponent implements Callable, MuleContextAware {

  private MuleContext muleContext;

  @Override
  public Object onCall(MuleEventContext eventContext) throws Exception {
    InputStream stream = (InputStream) muleContext.getTransformationService()
        .transform(eventContext.getMessage(), DataType.INPUT_STREAM).getPayload();
    stream.read();
    return "Hello";
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
