/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api;

/**
 * Enables {@link ToolingServiceAware} injection.
 *
 * @since 4.0
 */
public interface ToolingServiceAware {

  /**
   * @param toolingService the tooling service provided by the container
   */
  void setToolingService(ToolingService toolingService);

}
