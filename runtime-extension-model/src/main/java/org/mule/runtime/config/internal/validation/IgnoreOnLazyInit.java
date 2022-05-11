/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import org.mule.runtime.ast.api.validation.Validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link Validation} implementations annotated with this will not be evaluated when deploying with {@code lazyInit} enabled.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface IgnoreOnLazyInit {

  /**
   * Whether to force this validations via the deployment property
   * {@code mule.application.deployment.lazyInit.enableDslDeclarationValidations} even if running in lazy mode.
   */
  boolean forceDslDeclarationValidation() default false;
}
