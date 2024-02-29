/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.route;

import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.AT_LEAST_ONCE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.MULTIPLE_OR_NONE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.ONCE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.ONCE_OR_NONE;
import static org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence.UNKNOWN;

import org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.sdk.api.annotation.route.ExecutionOccurrence;

import java.util.Optional;

public final class JavaChainParsingUtils {

  public static ChainExecutionOccurrence parseChainExecutionOccurrence(Optional<AnnotationValueFetcher<ExecutionOccurrence>> value) {
    return value.map(a -> toModelApi(a.getEnumValue(ExecutionOccurrence::value))).orElse(UNKNOWN);
  }

  private static ChainExecutionOccurrence toModelApi(org.mule.sdk.api.annotation.route.ChainExecutionOccurrence value) {
    if (value == org.mule.sdk.api.annotation.route.ChainExecutionOccurrence.AT_LEAST_ONCE) {
      return AT_LEAST_ONCE;
    } else if (value == org.mule.sdk.api.annotation.route.ChainExecutionOccurrence.MULTIPLE_OR_NONE) {
      return MULTIPLE_OR_NONE;
    } else if (value == org.mule.sdk.api.annotation.route.ChainExecutionOccurrence.ONCE) {
      return ONCE;
    } else if (value == org.mule.sdk.api.annotation.route.ChainExecutionOccurrence.ONCE_OR_NONE) {
      return ONCE_OR_NONE;
    } else if (value == org.mule.sdk.api.annotation.route.ChainExecutionOccurrence.UNKNOWN) {
      return UNKNOWN;
    } else {
      throw new IllegalModelDefinitionException("Invalid value on @" + ExecutionOccurrence.class.getSimpleName() + ": " + value);
    }
  }
}
