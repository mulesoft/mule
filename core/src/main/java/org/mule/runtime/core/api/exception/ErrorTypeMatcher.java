/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils;

/**
 * Decides whether an error type is acceptable.
 *
 * @since 4.0
 * @deprecated use {@link org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher} instead.
 */
@Deprecated
public interface ErrorTypeMatcher extends org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher {

}
