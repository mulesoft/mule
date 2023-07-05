/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.common;

public class MuleProperties {

  public static final String SYSTEM_PROPERTY_PREFIX = "mule.";

  public static final String MULE_HOME_DIRECTORY_PROPERTY = SYSTEM_PROPERTY_PREFIX + "home";
  public static final String MULE_BASE_DIRECTORY_PROPERTY = SYSTEM_PROPERTY_PREFIX + "base";

  /**
   * If specified, the log separation feature will be disabled, resulting in a performance boost. This makes sense in deployment
   * models in which only one app will be deployed per runtime instance.
   * <p>
   * Log configuration file will only be fetched from {@code MULE_HOME/conf}. Deployed artifacts won't get their own file in the
   * {@code MULE_HOME/logs/} automatically.
   *
   * @since 1.3.0
   */
  public static final String MULE_LOG_SEPARATION_DISABLED = SYSTEM_PROPERTY_PREFIX + "disableLogSeparation";

  public static final String MULE_LOG_CONTEXT_DISPOSE_DELAY_MILLIS = SYSTEM_PROPERTY_PREFIX + "log.context.dispose.delay.millis";

  public static final String MULE_LOG_DEFAULT_POLICY_INTERVAL =
      SYSTEM_PROPERTY_PREFIX + "log.defaultAppender.timeBasedTriggerPolicy.interval";

  public static final String MULE_LOG_DEFAULT_STRATEGY_MAX = SYSTEM_PROPERTY_PREFIX + "log.defaultAppender.rolloverStrategy.max";
  public static final String MULE_LOG_DEFAULT_STRATEGY_MIN = SYSTEM_PROPERTY_PREFIX + "log.defaultAppender.rolloverStrategy.min";



}
