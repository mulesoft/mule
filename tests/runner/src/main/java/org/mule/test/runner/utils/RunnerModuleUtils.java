/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.utils;

import static org.mule.runtime.core.api.util.PropertiesUtils.discoverProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for runner.
 */
public final class RunnerModuleUtils {

  public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

  public static final String EXCLUDED_ARTIFACTS = "excluded.artifacts";
  public static final String EXTRA_BOOT_PACKAGES = "extraBoot.packages";
  public static final String JAR_EXTENSION = "jar";

  public static final String RUNNER_PROPERTIES_MULE_VERSION;

  static {
    try (final InputStream resourceAsStream = RunnerModuleUtils.class.getResourceAsStream("/runner.properties")) {
      Properties properties = new Properties();
      properties.load(resourceAsStream);
      RUNNER_PROPERTIES_MULE_VERSION = properties.getProperty("mule.version");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

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
    discoverProperties(EXCLUDED_PROPERTIES_FILE)
        .forEach(properties -> properties.forEach((k, v) -> excludedProperties.merge(k, v, (v1, v2) -> v1 + "," + v2)));
    return excludedProperties;
  }
}
