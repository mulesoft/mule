/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("Error Types")
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
        new ComponentIdentifier.Builder().withName("custom").withNamespace(CORE_NAMESPACE_NAME).build();
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
