/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoImplement;

/**
 * Allows for classifying a flow based on its name.
 *
 * @since 4.10
 */
@NoImplement
public interface FlowClassifier {

  enum FlowType {
    APIKIT, SOAPKIT, GENERIC
  }

  FlowType getFlowType(String flowName);
}
