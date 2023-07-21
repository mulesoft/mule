/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
