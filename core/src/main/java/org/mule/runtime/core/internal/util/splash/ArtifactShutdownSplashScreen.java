/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.applicationWasUpForDuration;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.artifactShutdownNormally;

import org.mule.runtime.core.api.MuleContext;

import java.util.Date;

public class ArtifactShutdownSplashScreen extends SplashScreen {

  @Override
  protected void doHeader(MuleContext context) {
    long currentTime = System.currentTimeMillis();
    header.add(artifactShutdownNormally(context.getArtifactType(), context.getConfiguration().getId(), new Date()).getMessage());
    long duration = 10;
    if (context.getStartDate() > 0) {
      duration = currentTime - context.getStartDate();
    }
    header.add(applicationWasUpForDuration(duration).getMessage());
  }
}


