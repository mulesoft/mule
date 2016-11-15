/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.worker;

import static org.mule.extension.socket.internal.SocketUtils.createResult;

import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.internal.SocketUtils;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.InputStream;
import java.util.function.Consumer;

public abstract class SocketWorker implements Disposable, Runnable {

  protected final SourceCallback<InputStream, SocketAttributes> callback;
  protected String encoding;
  private Consumer<Exception> errorHandler;

  protected SocketWorker(SourceCallback<InputStream, SocketAttributes> callback) {
    this.callback = callback;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void onError(Consumer<Exception> handler) {
    this.errorHandler = handler;
  }

  @Override
  public final void run() {
    try {
      doRun();
    } catch (Exception e) {
      errorHandler.accept(e);
    }
  }

  protected abstract void doRun() throws Exception;

  protected void handle(InputStream content, SocketAttributes attributes) {
    SourceCallbackContext ctx = callback.createContext();
    ctx.addVariable(SocketUtils.WORK, this);
    callback.handle(createResult(content, attributes), ctx);
  }

  public abstract void onComplete(Object result);

  public abstract void onError(Throwable e);
}
