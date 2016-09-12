/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.i18n;

import java.io.Serializable;

public class I18nMessage implements Serializable {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -6109760447384477924L;

  private String message;
  private int code = 0;
  private Object[] args;
  private I18nMessage nextMessage;

  protected I18nMessage(String message, int code, Object... args) {
    super();
    this.message = message;
    this.code = code;
    this.args = args;
  }

  public int getCode() {
    return code;
  }

  public Object[] getArgs() {
    return args;
  }

  public String getMessage() {
    return message + (nextMessage != null ? ". " + nextMessage.getMessage() : "");
  }

  public I18nMessage setNextMessage(I18nMessage nextMessage) {
    this.nextMessage = nextMessage;
    return this;
  }

  public I18nMessage getNextMessage() {
    return nextMessage;
  }

  public String toString() {
    return this.getMessage();
  }
}
