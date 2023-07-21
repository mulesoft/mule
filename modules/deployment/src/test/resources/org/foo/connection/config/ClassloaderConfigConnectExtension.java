/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.connection.config;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;

import org.foo.connection.config.ClassloaderConfigConnectionProvider;

/**
 * Extension for testing purposes
 */
@Extension(name = "ClassloaderConfigConnect")
@Operations({ClassloaderConfigOperation.class})
@ConnectionProviders(ClassloaderConfigConnectionProvider.class)
public class ClassloaderConfigConnectExtension {

  private String fileMessage;

  public String getFileMessage() {
    return fileMessage;
  }
}