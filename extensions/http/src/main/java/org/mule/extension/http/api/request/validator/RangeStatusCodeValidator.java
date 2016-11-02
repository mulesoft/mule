/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Base status code validator that can be extended to create custom validations.
 *
 * @since 4.0
 */
public abstract class RangeStatusCodeValidator implements ResponseValidator {

  /**
   * Status codes that will be considered.
   */
  @Parameter
  private String values;

  protected boolean belongs(int value) {
    String[] valueParts = values.split(",");

    for (String valuePart : valueParts) {
      if (valuePart.contains("..")) {
        String[] limits = valuePart.split("\\.\\.");
        int lower = Integer.parseInt(limits[0]);
        int upper = Integer.parseInt(limits[1]);

        if (value >= lower && value <= upper) {
          return true;
        }
      } else {
        int code = Integer.parseInt(valuePart);

        if (code == value) {
          return true;
        }
      }
    }

    return false;
  }

  public String getValues() {
    return values;
  }

  public void setValues(String values) {
    this.values = values;
  }
}
