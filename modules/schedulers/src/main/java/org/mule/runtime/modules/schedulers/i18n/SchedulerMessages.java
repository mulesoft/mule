/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.modules.schedulers.i18n;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class SchedulerMessages extends I18nMessageFactory {

  private static final SchedulerMessages factory = new SchedulerMessages();

  private static final String BUNDLE_PATH = getBundlePath("schedulers");

  public static I18nMessage couldNotCreateScheduler() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }

  public static I18nMessage invalidCronExpression() {
    return factory.createMessage(BUNDLE_PATH, 2);
  }

  public static I18nMessage couldNotScheduleJob() {
    return factory.createMessage(BUNDLE_PATH, 3);
  }

  public static I18nMessage couldNotPauseSchedulers() {
    return factory.createMessage(BUNDLE_PATH, 4);
  }

  public static I18nMessage couldNotShutdownScheduler() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }
}
