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
import static org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher.WILDCARD_TOKEN;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;

import org.junit.Test;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.AbstractErrorTypeMatcherTestCase;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
public class WildcardErrorTypeMatcherTestCase extends AbstractErrorTypeMatcherTestCase {

  @Test
  public void anyMatchesAll() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(transformationErrorType);
    ErrorTypeMatcher anyMatcher = new WildcardErrorTypeMatcher(anyErrorType);

    assertThat(anyMatcher.match(anyErrorType), is(true));
    assertThat(anyMatcher.match(transformationErrorType), is(true));
    assertThat(anyMatcher.match(expressionErrorType), is(true));
    assertThat(anyMatcher.match(mockErrorType), is(true));
  }

  @Test
  public void matchEqual() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(transformationErrorType), is(true));
  }

  @Test
  public void matchChild() {
    ComponentIdentifier customTransformerIdentifier =
        ComponentIdentifier.builder().name("custom").namespace(CORE_NAMESPACE_NAME).build();
    ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    ErrorType customTransformerErrorType = errorTypeRepository.addErrorType(customTransformerIdentifier, transformationErrorType);
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(customTransformerErrorType), is(true));
  }

  @Test
  public void doesNotMatchParent() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(anyErrorType), is(false));
  }

  @Test
  public void doesNotMatchSibling() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(transformationErrorType);

    assertThat(transformationMatcher.match(expressionErrorType), is(false));
  }

  @Test
  public void matchWildcardIdentifier() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(WILDCARD_TOKEN)
        .parentErrorType(transformationErrorType).build();
    ErrorType test1ErrorType = ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier("test1")
        .parentErrorType(transformationErrorType).build();
    ErrorType test2ErrorType = ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier("test2")
        .parentErrorType(transformationErrorType).build();
    ErrorType test3ErrorType =
        ErrorTypeBuilder.builder().namespace("unknown").identifier("test3").parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(test1ErrorType), is(true));
    assertThat(wildcardMatcher.match(test2ErrorType), is(true));
    assertThat(wildcardMatcher.match(test3ErrorType), is(false));
  }

  @Test
  public void matchWildcardNamespace() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(WILDCARD_TOKEN).identifier("fixedIdentifier")
        .parentErrorType(transformationErrorType).build();
    ErrorType test1ErrorType = ErrorTypeBuilder.builder().namespace("test1").identifier("fixedIdentifier")
        .parentErrorType(transformationErrorType).build();
    ErrorType test2ErrorType = ErrorTypeBuilder.builder().namespace("test2").identifier("fixedIdentifier")
        .parentErrorType(transformationErrorType).build();
    ErrorType test3ErrorType =
        ErrorTypeBuilder.builder().namespace("test3").identifier("unknown").parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(test1ErrorType), is(true));
    assertThat(wildcardMatcher.match(test2ErrorType), is(true));
    assertThat(wildcardMatcher.match(test3ErrorType), is(false));
  }

  @Test
  public void doesntMatchOtherNamespaceIfTheWildcardIsOnIdentifier() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(WILDCARD_TOKEN)
        .parentErrorType(transformationErrorType).build();

    ErrorType testErrorType = ErrorTypeBuilder.builder().namespace("otherThan" + CORE_NAMESPACE_NAME).identifier("test")
        .parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(false));
  }

  @Test
  public void doesntMatchOtherIdentifierIfTheWildcardIsOnNamespace() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(WILDCARD_TOKEN).identifier("anId")
        .parentErrorType(transformationErrorType).build();
    ErrorType testErrorType =
        ErrorTypeBuilder.builder().namespace("test").identifier("otherId").parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(false));
  }

  @Test
  public void matchDoubleWildcard() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(WILDCARD_TOKEN).identifier(WILDCARD_TOKEN)
        .parentErrorType(transformationErrorType).build();

    ErrorType test11ErrorType =
        ErrorTypeBuilder.builder().namespace("ns1").identifier("id1").parentErrorType(transformationErrorType).build();
    ErrorType test12ErrorType =
        ErrorTypeBuilder.builder().namespace("ns1").identifier("id2").parentErrorType(transformationErrorType).build();
    ErrorType test21ErrorType =
        ErrorTypeBuilder.builder().namespace("ns2").identifier("id1").parentErrorType(transformationErrorType).build();
    ErrorType test22ErrorType =
        ErrorTypeBuilder.builder().namespace("ns2").identifier("id2").parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);

    assertThat(wildcardMatcher.match(test11ErrorType), is(true));
    assertThat(wildcardMatcher.match(test12ErrorType), is(true));
    assertThat(wildcardMatcher.match(test21ErrorType), is(true));
    assertThat(wildcardMatcher.match(test22ErrorType), is(true));
  }

  @Test
  public void doesntMatchOtherParent() {
    ErrorType wildcardErrorType = ErrorTypeBuilder.builder().namespace(WILDCARD_TOKEN).identifier(WILDCARD_TOKEN)
        .parentErrorType(transformationErrorType).build();

    ErrorType test11ErrorType =
        ErrorTypeBuilder.builder().namespace("ns1").identifier("id1").parentErrorType(expressionErrorType).build();
    ErrorType test12ErrorType =
        ErrorTypeBuilder.builder().namespace("ns1").identifier("id2").parentErrorType(expressionErrorType).build();
    ErrorType test21ErrorType =
        ErrorTypeBuilder.builder().namespace("ns2").identifier("id1").parentErrorType(expressionErrorType).build();
    ErrorType test22ErrorType =
        ErrorTypeBuilder.builder().namespace("ns2").identifier("id2").parentErrorType(expressionErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);

    assertThat(wildcardMatcher.match(test11ErrorType), is(false));
    assertThat(wildcardMatcher.match(test12ErrorType), is(false));
    assertThat(wildcardMatcher.match(test21ErrorType), is(false));
    assertThat(wildcardMatcher.match(test22ErrorType), is(false));
  }

}
