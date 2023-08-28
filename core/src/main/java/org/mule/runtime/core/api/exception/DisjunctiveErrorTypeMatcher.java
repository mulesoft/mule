/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
