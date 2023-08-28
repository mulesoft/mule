/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.internal.util.splash.SplashScreen;

/**
 * Splash screen specific for {@link Service} startup based on the splash message provided by the service implementation.
 *
 * @since 4.1
 */
final class ServiceSplashScreen extends SplashScreen {

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

    for (String splashMessageLine : splash.split(lineSeparator())) {
      header.add(splashMessageLine);
    }
  }
}
