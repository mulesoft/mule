/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.domain;

import org.mule.api.transport.OutputHandler;

/**
 * Entity to wrap {@link OutputHandler OutputHandles}.
 *
 * @since 3.10
 */
public class OutputHandlerHttpEntity implements HttpEntity {

  private OutputHandler outputHandler;

  public OutputHandlerHttpEntity(OutputHandler outputHandler) {
    this.outputHandler = outputHandler;
  }

  public OutputHandler getOutputHandler() {
    return outputHandler;
  }

}
