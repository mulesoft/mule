/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import java.util.TimeZone;

import org.junit.rules.ExternalResource;

/**
 * Sets up a time zone to use on tests, guarantying that the original default one is reset afterwards.
 */
public class CustomTimeZone extends ExternalResource {

  private final TimeZone timeZone;
  private TimeZone savedTimeZone;

  public CustomTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  public CustomTimeZone(String ID) {
    this(TimeZone.getTimeZone(ID));
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  @Override
  protected void before() throws Throwable {
    savedTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(timeZone);
  }

  @Override
  protected void after() {
    TimeZone.setDefault(savedTimeZone);
  }

}
