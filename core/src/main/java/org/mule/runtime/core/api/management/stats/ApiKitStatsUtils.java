/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Utils to perform validations and operations related to ApiKit.
 *
 * @since 4.7.1
 */
public class ApiKitStatsUtils {

  private static final String APIKIT_FLOWNAME_REGEX =
      // method
      "(\\w*)" +
      // path
          ":(\\\\[^:]*)" +
          // content type
          "(:[^:]*)?" +
          // config name
          ":([^\\/\\\\\\[\\\\\\]\\{\\}#]*)";
  private static final String APIKIT_SOAP_FLOWNAME_REGEX =
      // method
      "(\\w*)" +
      // path
          ":\\\\" +
          // config name
          "([^\\/\\\\\\[\\\\\\]\\{\\}#]*)";
  private static final Pattern APIKIT_FLOWNAME_PATTERN = compile(APIKIT_FLOWNAME_REGEX);
  private static final Pattern APIKIT_SOAP_FLOWNAME_PATTERN = compile(APIKIT_SOAP_FLOWNAME_REGEX);

  /**
   * Determines if the name of a flow follows the conventions of ApiKit.
   *
   * @param flowName the name of the flow to check.
   * @return whether the name of the flow corresponds to an ApiKit flow.
   */
  public static boolean isApiKitFlow(String flowName) {
    return APIKIT_FLOWNAME_PATTERN.matcher(flowName).matches()
        || APIKIT_SOAP_FLOWNAME_PATTERN.matcher(flowName).matches();
  }

}
