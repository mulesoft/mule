/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import static java.lang.String.format;

import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 4.0
 */
public class CustomProtocol implements TcpProtocol, Initialisable {

  private static final Log LOGGER = LogFactory.getLog(CustomProtocol.class);

  private TcpProtocol delegate;

  /**
   * Reference to full qualifier class name that should extend {@link TcpProtocol} that will be used as a custom protocol
   */
  @Parameter
  @Alias("class")
  @Summary("Full qualifier class name that must implement 'TcpProtocol' that will be used as a custom protocol")
  @DisplayName("Protocol Class Name")
  public String clazz;

  public CustomProtocol() {}

  @Override
  public InputStream read(InputStream is) throws IOException {
    return delegate.read(is);
  }

  @Override
  public void write(OutputStream os, Object data, String encoding) throws IOException {
    delegate.write(os, data, encoding);
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      delegate = (TcpProtocol) ClassUtils.instanciateClass(clazz);
    } catch (Exception e) {
      throw new RuntimeException(format("Could not load class '%s'", clazz));
    }
  }
}
