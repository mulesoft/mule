/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.System.lineSeparator;
import static java.util.Collections.addAll;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.internal.util.splash.SplashScreen;

/**
 * Splash screen specific for {@link Service} startup based on the splash message provided by the service implementation.
 *
 * @since 4.1
 */
public final class ServiceSplashScreen extends SplashScreen {

  private final String serviceDescription;
  private final String splash;

  public ServiceSplashScreen(String serviceDescription, String splash) {
    this.serviceDescription = serviceDescription;
    this.splash = splash;
    doHeader();
  }

  private void doHeader() {
    header.add("Started " + serviceDescription);
    header.add("");

    addAll(header, splash.split(lineSeparator()));
  }
}
