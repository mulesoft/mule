/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.connection.operation;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;

import org.foo.connection.operation.ClassloaderConnectionProvider;

/**
 * Extension for testing purposes
 */
@Extension(name = "ClassloaderConnect")
@Operations({ClassloaderOperation.class})
@ConnectionProviders(ClassloaderConnectionProvider.class)
public class ClassloaderConnectExtension {

  private String fileMessage;

  public String getFileMessage() {
    return fileMessage;
  }
}