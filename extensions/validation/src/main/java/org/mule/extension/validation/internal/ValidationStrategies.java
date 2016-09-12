/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.extension.validation.api.ValidationExtension;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.extension.validation.api.MultipleValidationException;
import org.mule.extension.validation.api.MultipleValidationResult;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.runtime.core.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A class containing operations which performs validations according to different strategies
 *
 * @since 3.7.0
 */
public final class ValidationStrategies {

  /**
   * Perform a list of nested validation operations and informs only one {@link MultipleValidationResult} which summarizes all of
   * the found errors (if any).
   * <p/>
   * If {@code throwsException} is {@code true}, then the {@link ValidationResult} is communicated by throwing a
   * {@link ValidationException}. On the other hand, if {@code throwsException} is {@code false}, then the
   * {@link ValidationResult} is set as the message payload.
   * <p/>
   * When configured through XML, all the {@code validations} must include the All the child processors must contain the
   * {@code validator-message-processor} substitution group.
   *
   * @param validations the nested validation operations
   * @param muleEvent the current {@link Event}
   * @return the same {@code muleEvent} that was passed as argument
   * @throws MultipleValidationException if at least one validator fails and {@code throwsException} is {@code true}
   */
  public void all(@RestrictedTo(ValidationExtension.class) List<NestedProcessor> validations, Event muleEvent)
      throws MultipleValidationException {
    List<ValidationResult> results = new ArrayList<>(validations.size());
    for (NestedProcessor validation : validations) {
      try {
        validation.process();
      } catch (Exception e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause == null) {
          rootCause = e;
        }
        results.add(error(rootCause.getMessage()));
      }
    }

    MultipleValidationResult result = ImmutableMultipleValidationResult.of(results);

    if (result.isError()) {
      throw new MultipleValidationException(result);
    }
  }
}
