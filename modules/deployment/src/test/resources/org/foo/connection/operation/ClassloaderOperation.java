/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.connection.operation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import java.io.InputStream;

public class ClassloaderOperation {

  @MediaType(TEXT_PLAIN)
  public String fooConfigOperation1(@Config ClassloaderConnectExtension config){
    return "this operation receives the FooConfig!";
  }

  @MediaType(TEXT_PLAIN)
  public String fooConnectedOperation(@Connection ClassloaderConnection connection){
    return "this operation receives ClassConnection!";
  }

  @MediaType(TEXT_PLAIN)
  public String getFile(@Connection ClassloaderConnection connection){
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt");
    return IOUtils.toString(stream);
  }
}
