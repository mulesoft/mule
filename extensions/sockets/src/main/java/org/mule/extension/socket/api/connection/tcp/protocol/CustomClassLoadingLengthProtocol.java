/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.log4j.Logger;

/**
 * A length protocol that uses a specific class loader to load objects from streams
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
public class CustomClassLoadingLengthProtocol extends LengthProtocol {

  private final Logger logger = Logger.getLogger(this.getClass());

  @Parameter
  @Optional
  private ClassLoader classLoader;

  @Override
  public InputStream read(InputStream is) throws IOException {
    return new ClassLoaderObjectInputStream(this.getClassLoader(), is);
  }

  public ClassLoader getClassLoader() {
    if (this.classLoader == null) {
      this.classLoader = this.getClass().getClassLoader();
    }
    return classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }
}
