/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.util;


import static org.apache.commons.lang3.StringUtils.countMatches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class InvokerMessageProcessorUtil {

  private InvokerMessageProcessorUtil() {

  }

  /**
   * Split and merge comma separated mule expressions when they falls within an array
   * 
   * @param arguments arguments string expression
   * @return arguments merged
   */
  public static List<String> splitArgumentsExpression(String arguments) {
    String[] argumentsSplitted = arguments.split("\\s*,\\s*");
    if (argumentsSplitted.length <= 1) {
      return Arrays.asList(argumentsSplitted);
    }

    return merge(argumentsSplitted);
  }

  private static List<String> merge(String[] argumentsSplitted) {
    List<String> argumentsVerified = new ArrayList<String>();

    StringBuffer mergingArg = new StringBuffer();
    mergingArg.append(argumentsSplitted[0]);

    for (int index = 1; index < argumentsSplitted.length; index++) {
      String arg = argumentsSplitted[index];
      if (hasToBeMerged(mergingArg)) {
        mergingArg.append("," + arg);
      } else {
        argumentsVerified.add(mergingArg.toString().trim());
        mergingArg = new StringBuffer(arg);
      }
    }

    argumentsVerified.add(mergingArg.toString().trim());
    return argumentsVerified;
  }

  private static boolean hasToBeMerged(StringBuffer mergingArg) {
    return countMatches(mergingArg.toString(), "[") != countMatches(mergingArg.toString(), "]");
  }

}
