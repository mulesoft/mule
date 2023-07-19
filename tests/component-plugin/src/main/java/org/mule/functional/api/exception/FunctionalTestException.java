/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;

public class FunctionalTestException extends MuleException {

  public static final String EXCEPTION_MESSAGE = "Functional Test Service Exception";

  public FunctionalTestException() {
    this(EXCEPTION_MESSAGE);
  }

  public FunctionalTestException(String exceptionText) {
    super(createStaticMessage(exceptionText));
  }
}
