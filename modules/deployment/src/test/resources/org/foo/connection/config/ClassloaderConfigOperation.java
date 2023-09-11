/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.connection.config;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.InputStream;

public class ClassloaderConfigOperation {

  @MediaType(TEXT_PLAIN)
  public String fooConfigOperation1(@Config ClassloaderConfigConnectExtension config){
    return "this operation receives the FooConfig!";
  }

  @MediaType(TEXT_PLAIN)
  public String fooConnectedOperation(@Connection ClassloaderConfigConnection connection){
    return "this operation receives ClassConnection!";
  }

  @MediaType(TEXT_PLAIN)
  public String getFile(@Connection ClassloaderConfigConnection connection){
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt");
    return IOUtils.toString(stream);
  }

  @MediaType(TEXT_PLAIN)
  public String getFileObtainedAtConfig(@Config ClassloaderConfigConnectExtension config, @Connection ClassloaderConfigConnection connection){
    return connection.getFileContent();
  }
}
