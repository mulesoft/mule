/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class FlowConstructInvalidException extends MuleException {

  private static final long serialVersionUID = -8170840339166473625L;

  public FlowConstructInvalidException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public FlowConstructInvalidException(I18nMessage message) {
    super(message);
  }

  public FlowConstructInvalidException(I18nMessage message, FlowConstruct flowConstruct) {
    super(message);
    addInfo("FlowConstruct", flowConstruct);
  }

  public FlowConstructInvalidException(Throwable cause) {
    super(cause);
  }

}
