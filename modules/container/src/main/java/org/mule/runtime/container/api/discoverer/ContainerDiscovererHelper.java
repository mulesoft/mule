/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api.discoverer;

/**
 * No-op implementation of ContainerDiscovererHelper to use when running on JVM 8.
 * 
 * @since 4.6
 */
public final class ContainerDiscovererHelper {

  private ContainerDiscovererHelper() {
    // nothing to do
  }

  public static void exportInternalsToTestRunner() {
    // nothing to do
  }
}
