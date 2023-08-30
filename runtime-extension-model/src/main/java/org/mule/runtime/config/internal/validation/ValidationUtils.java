/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_COLUMN;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_END_POSITION_OFFSET;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_COLUMN;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_LINE;
import static org.mule.runtime.config.api.validation.ExpressionsSyntacticallyValidAdditionalDataKeys.LOCATION_START_POSITION_OFFSET;

import org.mule.runtime.api.el.validation.Location;

import java.util.HashMap;
import java.util.Map;


class ValidationUtils {

  private ValidationUtils() {
    // do nothing
  }

  public static Map<String, String> locationToAdditionalData(Location location) {
    Map<String, String> additionalData = new HashMap<>();

    additionalData.put(LOCATION_START_POSITION_LINE,
                       Integer.toString(location.getStartPosition().getLine()));
    additionalData.put(LOCATION_START_POSITION_COLUMN,
                       Integer.toString(location.getStartPosition().getColumn()));
    additionalData.put(LOCATION_START_POSITION_OFFSET,
                       Integer.toString(location.getStartPosition().getOffset()));
    additionalData.put(LOCATION_END_POSITION_LINE,
                       Integer.toString(location.getEndPosition().getLine()));
    additionalData.put(LOCATION_END_POSITION_COLUMN,
                       Integer.toString(location.getEndPosition().getColumn()));
    additionalData.put(LOCATION_END_POSITION_OFFSET,
                       Integer.toString(location.getEndPosition().getOffset()));

    return additionalData;

  }
}
