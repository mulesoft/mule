/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.AbstractErrorTypeMatcherTestCase;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
public class SingleErrorTypeMatcherTestCase extends AbstractErrorTypeMatcherTestCase {

  @Test
  public void anyMatchsAll() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(transformationErrorType);
    ErrorTypeMatcher anyMatcher = new SingleErrorTypeMatcher(anyErrorType);

    assertThat(anyMatcher.match(anyErrorType), is(true));
    assertThat(anyMatcher.match(transformationErrorType), is(true));
    assertThat(anyMatcher.match(expressionErrorType), is(true));
    assertThat(anyMatcher.match(mockErrorType), is(true));
  }

  @Test
  public void matchEqual() {
    ErrorTypeMatcher transformationMatcher = new SingleErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(transformationErrorType), is(true));
  }

  @Test
  public void matchChild() {
    ComponentIdentifier customTransformerIdentifier =
        ComponentIdentifier.builder().name("custom").namespace(CORE_NAMESPACE_NAME).build();
    ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    ErrorType customTransformerErrorType = errorTypeRepository.addErrorType(customTransformerIdentifier, transformationErrorType);
    ErrorTypeMatcher transformationMatcher = new SingleErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(customTransformerErrorType), is(true));
  }

  @Test
  public void doesNotMatchParent() {
    ErrorTypeMatcher transformationMatcher = new SingleErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(anyErrorType), is(false));
  }

  @Test
  public void doesNotMatchSibling() {
    ErrorTypeMatcher transformationMatcher = new SingleErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(expressionErrorType), is(false));
  }

}
