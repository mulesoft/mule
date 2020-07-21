/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.hello;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static java.lang.Class.forName;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

public class HelloOperation {

  public HelloOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config HelloExtension config) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    return config.getMessage();
  }

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String connect(@Config HelloExtension config) {
    try{
      //step1 load the driver class
      forName("oracle.jdbc.driver.OracleDriver");

      //step2 create  the connection object
      //Connection con = DriverManager.getConnection(
      //        "jdbc:oracle:thin:@localhost:1521:xe","system","oracle");
      //con.close();

      return config.getMessage();

    } catch(Exception e) {
      return "Exception ocurred: " + e.getMessage();
    }
}
