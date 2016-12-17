/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.config;

import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.extension.socket.api.source.SocketListener;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import javax.inject.Inject;

/**
 * Abstract config for {@link SocketsExtension}
 * 
 * @since 4.0
 */
public class AbstractSocketConfig {

  /**
   * Default encoding used for serializing {@link String}. This encoding is used in
   * {@link SocketOperations#send(RequesterConnection, RequesterConfig, Object, String, String, Message)} operation for
   * serializing {@link String} types if no encoding parameter is specified.
   *
   * It is also used by the {@link SocketWorker} for encoding the replies send by the {@link SocketListener} source.
   */
  @Parameter
  @Optional
  @DefaultEncoding
  private String defaultEncoding;

  public String getDefaultEncoding() {
    return defaultEncoding;
  }
}
