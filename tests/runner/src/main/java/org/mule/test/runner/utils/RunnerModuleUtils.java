/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for runner.
 */
public final class RunnerModuleUtils {

  public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

  public static final String EXCLUDED_ARTIFACTS = "excluded.artifacts";
  public static final String EXTRA_BOOT_PACKAGES = "extraBoot.packages";

  // TODO: MULE-19762 remove once forward compatiblity is finished
  private static String DEFAULT_TEST_SDK_API_VERSION_PROPERTY = SYSTEM_PROPERTY_PREFIX + "testSdkApiVersion";

  private RunnerModuleUtils() {}

  /**
   * Loads the {@link RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} resources files, merges the entries so only one
   * {@link Properties} is returned with all values.
   *
   * @return a {@link Properties} loaded with the content of the file.
   * @throws IOException           if the properties couldn't load the file.
   * @throws IllegalStateException if the file couldn't be found.
   */
  public static Properties getExcludedProperties() throws IllegalStateException, IOException {
    Properties excludedProperties = new Properties();
    discoverProperties(EXCLUDED_PROPERTIES_FILE).stream()
        .forEach(properties -> properties.forEach((k, v) -> excludedProperties.merge(k, v, (v1, v2) -> v1 + "," + v2)));
    return excludedProperties;
  }

  /**
   * @return resolves the default version of {@code mule-sdk-api} to add into the container classpath
   */
  // TODO: MULE-19762 remove once forward compatiblity is finished
  public static String getDefaultSdkApiVersionForTest() {
    return getProperty(DEFAULT_TEST_SDK_API_VERSION_PROPERTY, "0.4.0");
  }
}
