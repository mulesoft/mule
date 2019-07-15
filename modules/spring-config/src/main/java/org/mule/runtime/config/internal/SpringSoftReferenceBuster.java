/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.beans.Introspector.flushCaches;

import org.mule.module.artifact.classloader.soft.buster.SoftReferenceBuster;

/**
 * Reference buster to flush caches used by spring introspection of classes
 * 
 * @since 4.2.2
 */
public class SpringSoftReferenceBuster implements SoftReferenceBuster {

  @Override
  public void bustSoftReferences(ClassLoader classLoader) {
    flushCaches();
  }

}
