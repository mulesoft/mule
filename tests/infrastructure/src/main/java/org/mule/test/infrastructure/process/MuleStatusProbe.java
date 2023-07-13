/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process;

import org.mule.tck.probe.Probe;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleStatusProbe implements Probe {

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleStatusProbe.class);

  private final MuleProcessController controller;
  private final boolean check;

  private MuleStatusProbe(MuleProcessController controller, boolean isRunning) {
    this.controller = controller;
    this.check = isRunning;
  }

  @Override
  public boolean isSatisfied() {
    return check == controller.isRunning();
  }

  @Override
  public String describeFailure() {
    try {
      controller.getController().printLog();
    } catch (IOException e) {
      LOGGER.error("Error printing log.", e);
    }

    return "Mule is " + (check ? "not " : "") + "running";
  }

  public static Probe isRunning(MuleProcessController controller) {
    return new MuleStatusProbe(controller, true);
  }

  public static Probe isNotRunning(MuleProcessController controller) {
    return new MuleStatusProbe(controller, false);
  }

}
