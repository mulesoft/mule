/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Text;

/**
 * Parameters to configure an operation which executes a SQL script
 *
 * @since 4.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class BulkScript {

  /**
   * The text of the SQL query to be executed
   */
  @Parameter
  @Optional
  @Text
  @DisplayName("SQL Query Text")
  private String sql;

  /**
   * The location of a file to load. The file can point to a resource on the classpath or on a disk.
   */
  @Parameter
  @Optional
  @DisplayName("Script path")
  private String file;


  public String getSql() {
    return sql;
  }

  public String getFile() {
    return file;
  }
}
