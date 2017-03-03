/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.http.internal.request;

import org.mule.runtime.core.api.scheduler.SchedulerService;

/**
 * Factory object for {@link HttpClient} mule-to-httpLibrary adapters.
 */
public interface HttpClientFactory {

  /**
   * @param configuration the configuration to use for the underlying http library.
   * @param schedulerService the provider of the thread pools to be used by the grizzly client
   * @return a newly built {@link HttpClient}
   */
  HttpClient create(HttpClientConfiguration configuration, SchedulerService schedulerService);

}
