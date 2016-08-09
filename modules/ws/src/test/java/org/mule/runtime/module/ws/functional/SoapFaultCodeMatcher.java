/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import org.mule.runtime.module.ws.consumer.SoapFaultException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class SoapFaultCodeMatcher extends TypeSafeMatcher<SoapFaultException> {

  private final String code;

  public SoapFaultCodeMatcher(String code) {
    this.code = code;
  }

  @Override
  protected boolean matchesSafely(SoapFaultException item) {
    return code.equals(item.getFaultCode().getLocalPart());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a fault code " + code);
  }

  @Override
  protected void describeMismatchSafely(SoapFaultException item, Description mismatchDescription) {
    mismatchDescription.appendText("is not a SoapFaultException with fault code ")
        .appendValue(item.getFaultCode().getLocalPart());
  }

  public static SoapFaultCodeMatcher hasFaultCode(final String code) {
    return new SoapFaultCodeMatcher(code);
  }
}
