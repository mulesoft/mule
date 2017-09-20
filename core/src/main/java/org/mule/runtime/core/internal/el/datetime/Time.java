/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
