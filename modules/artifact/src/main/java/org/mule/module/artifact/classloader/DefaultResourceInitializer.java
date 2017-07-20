/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.runtime.module.artifact.classloader.ResourceInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation used for every Mule container.
 */
public class DefaultResourceInitializer implements ResourceInitializer {

  private final transient Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize() {
    try {
      Class.forName("javax.xml.bind.DatatypeConverterImpl");
    } catch (ClassNotFoundException e) {
      // Nothing to do...
    } catch (Throwable t) {
      logger.warn("Couldn't initialize DataTypeConverterImpl in order to prevent a class loader leak", t);
    }
  }

}
