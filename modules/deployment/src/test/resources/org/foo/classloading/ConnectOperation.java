/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.classloading;

import static java.lang.Thread.currentThread;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.annotation.param.Config;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class ConnectOperation {

  public String fooConfigOperation(@Config ConnectExtension config){
    return "this operation receives the FooConfig!";
  }

  public String fooConnectedOperation(@Connection ClassConnection123 connection){
    return "this operation receives ClassConnection!";
  }

  public String fooConnectedOperation(@Config ConnectExtension config, @Connection ClassConnection123 connection){
    return "this operation receives both config and connection!";
  }

}
