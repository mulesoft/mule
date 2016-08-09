/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.support;

/**
 * Factory for instantiating JMX helper classes.
 */
public interface JmxSupportFactory {

  /**
   * Create an instance of a JMX support class.
   * 
   * @return class instance
   */
  JmxSupport getJmxSupport();
}
