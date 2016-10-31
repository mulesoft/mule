/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.exceptions;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class FunctionalTestException extends MuleException {

  public static final String EXCEPTION_MESSAGE = "Functional Test Service Exception";

  public FunctionalTestException() {
    this(EXCEPTION_MESSAGE);
  }

  public FunctionalTestException(String exceptionText) {
    super(I18nMessageFactory.createStaticMessage(exceptionText));
  }
}
