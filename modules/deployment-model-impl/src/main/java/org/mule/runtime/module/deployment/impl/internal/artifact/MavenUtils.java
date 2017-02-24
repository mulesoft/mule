/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.System.getProperty;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO(pablo.kraan): embedded - remove class duplication
class MavenUtils {

  private static final String USER_HOME = "user.home";
  private static final String M2_REPO = "/.m2/repository";
  private static String userHome = getProperty(USER_HOME);

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

  private MavenUtils() {}

  public static File getMavenLocalRepository() {
    String localRepositoryProperty = getProperty("localRepository");
    if (localRepositoryProperty == null) {
      localRepositoryProperty = userHome + M2_REPO;
      LOGGER.debug("System property 'localRepository' not set, using Maven default location: $USER_HOME{}", M2_REPO);
    }

    LOGGER.debug("Using Maven localRepository: '{}'", localRepositoryProperty);
    File mavenLocalRepositoryLocation = new File(localRepositoryProperty);
    if (!mavenLocalRepositoryLocation.exists()) {
      throw new IllegalArgumentException("Maven repository location couldn't be found, please check your configuration");
    }
    return mavenLocalRepositoryLocation;
  }

}
