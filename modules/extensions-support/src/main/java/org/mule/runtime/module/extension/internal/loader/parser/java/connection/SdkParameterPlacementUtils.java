/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement;

/**
 * Helper class to handle http parameter placements.
 *
 * @since 4.5.0
 */
public class SdkParameterPlacementUtils {

  /**
   * @param parameterPlacement the http parameter placement to translate to the extensions-api enum.
   * @return the corresponding http parameter placement from the extensions-api that derives the given one.
   */
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
