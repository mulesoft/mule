/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.schedule;


import static org.mule.runtime.core.source.polling.PollingMessageSource.POLLING_SCHEME;

import org.mule.runtime.core.util.Predicate;

/**
 * <p>
 * Utility class to create {@link Scheduler} predicates
 * </p>
 */
public class Schedulers {

  /**
   * @return Predicate used to request the {@link org.mule.runtime.core.api.registry.MuleRegistry} all the polling
   *         {@link org.mule.runtime.core.api.schedule.Scheduler}
   */
  public static Predicate<String> allPollSchedulers() {
    return new Predicate<String>() {

      @Override
      public boolean evaluate(String s) {
        return s.startsWith(POLLING_SCHEME + "://");
      }
    };
  }

  /**
   * @return Predicate used to request the {@link org.mule.runtime.core.api.registry.MuleRegistry} all the polling
   *         {@link org.mule.runtime.core.api.schedule.Scheduler} for a particular
   *         {@link org.mule.runtime.core.api.construct.FlowConstruct}
   */
  public static Predicate<String> flowConstructPollingSchedulers(final String flowConstruct) {
    return new Predicate<String>() {

      @Override
      public boolean evaluate(String s) {
        return s.startsWith(POLLING_SCHEME + "://" + flowConstruct + "/");
      }
    };
  }
}
