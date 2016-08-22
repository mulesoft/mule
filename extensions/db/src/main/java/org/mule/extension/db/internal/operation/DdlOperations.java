/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Text;

public class DdlOperations {

  /**
   * Enables execution of DDL queries against a database.
   *
   * @param dynamicQuery
   * @param settings
   */
  public void executeDdl(@Text String dynamicQuery, @ParameterGroup QuerySettings settings) {

  }
}
