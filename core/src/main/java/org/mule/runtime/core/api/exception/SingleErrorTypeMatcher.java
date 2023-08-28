/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils;

/**
 * @deprecated create the {@link org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher} using
 *             {@link ErrorTypeMatcherUtils#createErrorTypeMatcher(ErrorTypeRepository, String)}
 */
@Deprecated
public final class SingleErrorTypeMatcher extends org.mule.runtime.api.message.error.matcher.SingleErrorTypeMatcher
    implements ErrorTypeMatcher {

  public SingleErrorTypeMatcher(ErrorType errorType) {
    super(errorType);
  }

}
