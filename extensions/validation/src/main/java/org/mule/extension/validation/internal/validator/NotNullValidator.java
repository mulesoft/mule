/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.validator;

import static org.mule.extension.validation.internal.ImmutableValidationResult.ok;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.internal.ValidationContext;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * An {@link AbstractValidator} which verifies that a {@link #value} is not {@code null}
 *
 * @since 3.7.0
 */
public class NotNullValidator extends AbstractValidator {

  private final Object value;

  public NotNullValidator(Object value, ValidationContext validationContext) {
    super(validationContext);
    this.value = value;
  }

  @Override
  public ValidationResult validate() {
    return value != null ? ok() : fail();
  }

  @Override
  protected I18nMessage getDefaultErrorMessage() {
    return getMessages().valueIsNull();
  }
}
