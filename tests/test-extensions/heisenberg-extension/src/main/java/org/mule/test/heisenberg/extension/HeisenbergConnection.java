/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
