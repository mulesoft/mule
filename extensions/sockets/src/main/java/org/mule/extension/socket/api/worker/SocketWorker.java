/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.worker;

import org.mule.extension.socket.api.SocketAttributes;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.extension.api.runtime.MessageHandler;

import java.io.InputStream;

import javax.resource.spi.work.Work;

public abstract class SocketWorker implements Disposable, Work {

  protected final MessageHandler<InputStream, SocketAttributes> messageHandler;
  protected String encoding;

  protected SocketWorker(MessageHandler<InputStream, SocketAttributes> messageHandler) {
    this.messageHandler = messageHandler;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }
}
