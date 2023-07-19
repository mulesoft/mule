/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.datetime;

public interface Date extends Instant {

  int getDayOfWeek();

  int getDayOfMonth();

  int getDayOfYear();

  int getWeekOfMonth();

  int getWeekOfYear();

  int getMonth();

  int getYear();

  Date plusDays(int add);

  Date plusWeeks(int add);

  Date plusMonths(int add);

  Date plusYears(int add);

  @Override
  Date withTimeZone(String newTimezone);

  @Override
  Date changeTimeZone(String newTimezone);

  @Override
  Date withLocale(String locale);

}
