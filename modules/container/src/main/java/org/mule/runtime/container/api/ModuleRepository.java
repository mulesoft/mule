/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import java.util.List;

/**
 * Provides access to all Mule modules available on the container.
 */
public interface ModuleRepository {

  /**
   * @return a non null list of {@link MuleModule}
   */
  List<MuleModule> getModules();
}
