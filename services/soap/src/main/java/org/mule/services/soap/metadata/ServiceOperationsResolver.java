/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.metadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Body parts of a web service operation.
 *
 * @since 4.0
 */
final class ServiceOperationsResolver {

  private final WsdlIntrospecter introspecter;

  ServiceOperationsResolver(WsdlIntrospecter introspecter) {
    this.introspecter = introspecter;
  }

  Set<String> getAvailableOperations() {
    return new HashSet<>(introspecter.getOperationNames());
  }
}
