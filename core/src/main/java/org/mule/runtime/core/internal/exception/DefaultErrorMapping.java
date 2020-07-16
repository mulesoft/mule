/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.meta.model.operation.ErrorMappings.ErrorMapping;

public final class DefaultErrorMapping implements ErrorMapping {

  private final String source;
  private final String target;

  public DefaultErrorMapping(String source, String target) {
    this.source = source;
    this.target = target;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public String getTarget() {
    return target;
  }

}
