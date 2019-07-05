/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader.soft.buster;

/**
 * Interface to perform cleaning necessary to avoid soft reference directly or indirectly retaining mule classloaders
 * 
 * @since 4.2.3
 */
public interface SoftReferenceBuster {

  void bustSoftReferences(ClassLoader classLoader);
}
