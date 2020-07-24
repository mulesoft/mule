/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.oracle;

import static java.lang.Thread.sleep;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.sql.DriverManager.getConnection;

import java.lang.Class;
import java.lang.Thread;
import java.sql.Connection;
import oracle.jdbc.driver.OracleDriver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

public class OracleOperation {

  private static final String driverClassName = "oracle.jdbc.driver.OracleDriver";

  public OracleOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config OracleExtension config) {
    return "Test plugin extension says hello!";
  }

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String connect(@Config OracleExtension config) {
    try {
      Class.forName(this.driverClassName);

      Connection con = getConnection(config.getUrl(), config.getUser(), config.getPassword());

      con.close();

      return "Connection success!";

    } catch (Exception e) {
      return "Exception ocurred while attempting to connect: " + e.getMessage();
    }
  }
}
