/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public final class FlowConstructInvalidException extends MuleException {

  private static final long serialVersionUID = -8170840339166473623L;

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
