/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementations of this class should take care of resources initialization with System Class Loader
 * as if they are loaded with application's Class Loader it may leak memory after application is undeployment. Mule
 * ensures to create an instance of this class with the System Class Loader.
 *
 * @since 4.0
 */
public class DefaultResourceInitializer {

  private final transient Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Attempts to initialize resources that should not be initialized from an application class loader.
   * system or parent class loader is the {@link Thread#contextClassLoader} of the current
   * thread when method is invoked.
   */
  public void initialize() {
    // When plugins have com.sun.xml.bind:jaxb-impl:jar a reference to the MuleArtifactClassLoader (plugin) will be
    // referenced by com.sun.xml.bind.DatatypeConverterImpl. Loading this class with the system or container class loader
    // will prevent this class loader leak.
    try {
      Class.forName("javax.xml.bind.DatatypeConverterImpl");
    } catch (ClassNotFoundException e) {
      // Nothing to do...
    } catch (Throwable t) {
      logger.warn("Couldn't initialize DataTypeConverterImpl in order to prevent a class loader leak", t);
    }
  }

}
