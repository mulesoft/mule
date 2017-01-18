/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.mule.compatibility.transport.ssl.SslConnector.LOCAL_CERTIFICATES;

import org.mule.functional.functional.EventCallback;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SaveCertificateCallback implements EventCallback {

  private AtomicReference<Object> certificates;
  private AtomicBoolean called;

  public SaveCertificateCallback() {
    clear();
  }

  @Override
  public void eventReceived(MuleEventContext context, Object component, MuleContext muleContext) throws Exception {
    certificates.set(((InternalMessage) context.getMessage()).getOutboundProperty(LOCAL_CERTIFICATES));
    called.set(true);
  }

  public void clear() {
    certificates = new AtomicReference<>();
    called = new AtomicBoolean(false);
  }

  public boolean isCalled() {
    return called.get();
  }

  public Object getCertificates() {
    return certificates.get();
  }
}
