/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;

public class HeisenbergConnection implements Lifecycle {

  private boolean connected = true;
  private final String saulPhoneNumber;

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

  @Override
  public void dispose() {
    dispose++;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialise++;
  }

  @Override
  public void start() throws MuleException {
    start++;
  }

  @Override
  public void stop() throws MuleException {
    stop++;
  }

  public void verifyLifecycle(int init, int start, int stop, int dispose) {
    verifyLifecycle("init", initialise, init);
    verifyLifecycle("start", this.start, start);
    verifyLifecycle("stop", this.stop, stop);
    verifyLifecycle("dispose", this.dispose, dispose);
  }

  private void verifyLifecycle(String phaseName, int value, int expected) {
    if (value != expected) {
      throw new IllegalStateException(String.format(
                                                    "lifecycle phase '%s' wrongfully applied. Was expecting to be applied %d times but %d found instead",
                                                    phaseName, expected, value));
    }
  }
}
