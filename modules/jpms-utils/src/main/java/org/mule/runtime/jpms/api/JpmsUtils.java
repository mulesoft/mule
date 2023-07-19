/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.jpms.api;

import java.util.Set;

/**
 * No-op implementation of JpmsUtils to use when running on JVM 8.
 * 
 * @since 4.5
 */
public final class JpmsUtils {

  private JpmsUtils() {
    // Nothing to do
  }

  public static void exploreJdkModules(Set<String> packages) {
    // nothing to do
  }

  public static void validateNoBootModuleLayerTweaking() {
    // nothing to do
  }

}
