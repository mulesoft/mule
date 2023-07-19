/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.datetime;

public interface Time extends Instant {

  long getMilliSeconds();

  int getSeconds();

  int getMinutes();

  int getHours();

  Time plusMilliSeconds(int add);

  Time plusSeconds(int add);

  Time plusMinutes(int add);

  Time plusHours(int add);

  @Override
  Time withTimeZone(String newTimezone);

  @Override
  Time changeTimeZone(String newTimezone);

  @Override
  Time withLocale(String locale);

}
