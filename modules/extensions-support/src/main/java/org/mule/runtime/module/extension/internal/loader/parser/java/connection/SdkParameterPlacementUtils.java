/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement;

// ADD JDOC
public class SdkParameterPlacementUtils {

  // ADD JDOC
  public static HttpParameterPlacement from(org.mule.sdk.api.runtime.parameter.HttpParameterPlacement parameterPlacement) {
    switch (parameterPlacement) {
      case QUERY_PARAMS:
        return HttpParameterPlacement.QUERY_PARAMS;
      case HEADERS:
        return HttpParameterPlacement.HEADERS;
      default:
        return HttpParameterPlacement.QUERY_PARAMS;
    }
  }

}
