/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.test.values.extension.connection.AbstractParamConnection;

public class AbstractParamOperations {

  public String abstractParamValue(@Connection AbstractParamConnection connection) {
    return connection.getName();
  }
}
