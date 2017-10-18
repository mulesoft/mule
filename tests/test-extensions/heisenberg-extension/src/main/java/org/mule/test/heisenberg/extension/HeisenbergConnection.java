/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.api.tls.TlsContextFactory;

public class HeisenbergConnection {

  private boolean connected = true;
  private final String saulPhoneNumber;
  private TlsContextFactory tlsContextFactory;

  private int initialise = 0;
  private int start = 0;
  private int stop = 0;
  private int dispose;

  public HeisenbergConnection(String saulPhoneNumber) {
    this.saulPhoneNumber = saulPhoneNumber;
  }

  public String callSaul() {
    return "You called " + saulPhoneNumber;
  }

  public void disconnect() {
    connected = false;
  }

  public boolean isConnected() {
    return connected;
  }

  public String getSaulPhoneNumber() {
    return saulPhoneNumber;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }

  public void setTlsContextFactory(TlsContextFactory tlsContextFactory) {
    this.tlsContextFactory = tlsContextFactory;
  }
}
