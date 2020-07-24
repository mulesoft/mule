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
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

public class OracleOperation {

  public OracleOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config OracleExtension config) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    return config.getMessage();
  }

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String connect(@Config OracleExtension config) {
    try {
      Class.forName("oracle.jdbc.driver.OracleDriver");

      Connection con = getConnection("jdbc:oracle:thin:@//localhost:49161/xe","system","oracle");

      con.close();

      return config.getMessage();

    } catch (Exception e) {
      return "Exception ocurred: " + e.getMessage();
    }
  }
}
