/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.message.ErrorType;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(ERROR_HANDLING)
@Stories(ERROR_TYPES)
public class DisjunctiveErrorTypeMatcherTestCase extends AbstractErrorTypeMatcherTestCase {


  @Test
  public void anyPresenceMatchAll() {
    ErrorTypeMatcher matcherWithAny = createMatcher(anyErrorType, transformationErrorType);

    assertThat(matcherWithAny.match(anyErrorType), is(true));
    assertThat(matcherWithAny.match(transformationErrorType), is(true));
    assertThat(matcherWithAny.match(expressionErrorType), is(true));
  }

  @Test
  public void oneMatch() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(transformationErrorType);
    ErrorTypeMatcher matcherWithTransformation = createMatcher(transformationErrorType, expressionErrorType);

    assertThat(matcherWithTransformation.match(transformationErrorType), is(true));
    assertThat(matcherWithTransformation.match(mockErrorType), is(true));
  }

  @Test
  public void allMatch() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(transformationErrorType);
    ErrorTypeMatcher matcherWithTwoTransformation = createMatcher(transformationErrorType, mockErrorType);

    assertThat(matcherWithTwoTransformation.match(mockErrorType), is(true));
  }

  @Test
  public void noMatch() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(anyErrorType);
    ErrorTypeMatcher matcherWithTwoTransformation = createMatcher(transformationErrorType, expressionErrorType);

    assertThat(matcherWithTwoTransformation.match(mockErrorType), is(false));
  }

  private ErrorTypeMatcher createMatcher(ErrorType... errorTypes) {
    return new DisjunctiveErrorTypeMatcher(stream(errorTypes).map(SingleErrorTypeMatcher::new).collect(toList()));
  }

}
