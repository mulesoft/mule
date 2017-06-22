/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


