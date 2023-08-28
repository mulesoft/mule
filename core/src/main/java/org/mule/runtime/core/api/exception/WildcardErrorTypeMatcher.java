/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils;

/**
 * @deprecated create the {@link org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher} using
 *             {@link ErrorTypeMatcherUtils#createErrorTypeMatcher(ErrorTypeRepository, String)}
 */
@Deprecated
public final class WildcardErrorTypeMatcher extends org.mule.runtime.api.message.error.matcher.WildcardErrorTypeMatcher
    implements ErrorTypeMatcher {

  public WildcardErrorTypeMatcher(ComponentIdentifier errorTypeIdentifier) {
    super(errorTypeIdentifier);
  }

}
