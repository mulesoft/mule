/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
