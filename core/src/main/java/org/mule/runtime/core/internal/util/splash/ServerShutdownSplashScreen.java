/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.splash;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.util.Date;

public class ServerShutdownSplashScreen extends SplashScreen {

  protected void doHeader(MuleContext context) {
    long currentTime = System.currentTimeMillis();
    header.add(CoreMessages.shutdownNormally(new Date()).getMessage());
    long duration = 10;
    if (context.getStartDate() > 0) {
      duration = currentTime - context.getStartDate();
    }
    header.add(CoreMessages.serverWasUpForDuration(duration).getMessage());
  }
}


