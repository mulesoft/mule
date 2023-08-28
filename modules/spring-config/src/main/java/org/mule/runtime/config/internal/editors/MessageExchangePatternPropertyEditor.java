/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.editors;

import org.mule.runtime.core.api.MessageExchangePattern;

import java.beans.PropertyEditorSupport;

public class MessageExchangePatternPropertyEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    setValue(MessageExchangePattern.fromString(text));
  }
}
