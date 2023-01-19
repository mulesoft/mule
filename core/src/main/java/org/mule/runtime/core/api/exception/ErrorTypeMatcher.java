/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.matcher.ErrorTypeMatcherUtils;

/**
 * Decides whether an error type is acceptable.
 *
 * @since 4.0
 * @deprecated create the {@link org.mule.runtime.api.message.matcher.ErrorTypeMatcher} using
 *             {@link ErrorTypeMatcherUtils#createErrorTypeMatcher(ErrorTypeRepository, String)}
 */
@Deprecated
public interface ErrorTypeMatcher extends org.mule.runtime.api.message.matcher.ErrorTypeMatcher {

}
