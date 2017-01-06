/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.lifecycle.Callable;

import java.io.ByteArrayInputStream;

/**
 * A simple component which returns a stream.
 */
public class StreamingResponseComponent implements Callable {

  @Override
  public Object onCall(MuleEventContext eventContext) throws Exception {
    return new ByteArrayInputStream("hello".getBytes());
  }
}
