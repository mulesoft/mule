/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils;

import static java.util.stream.Collectors.toList;
import java.util.List;

/**
 * @deprecated create the {@link org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher} using
 *             {@link ErrorTypeMatcherUtils#createErrorTypeMatcher(ErrorTypeRepository, String)}
 */
@Deprecated
public final class DisjunctiveErrorTypeMatcher extends org.mule.runtime.api.message.error.matcher.DisjunctiveErrorTypeMatcher
    implements ErrorTypeMatcher {

  public DisjunctiveErrorTypeMatcher(List<ErrorTypeMatcher> errorTypeMatchers) {
    super(errorTypeMatchers.stream().map(em -> (org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher) em)
        .collect(toList()));
  }

}
