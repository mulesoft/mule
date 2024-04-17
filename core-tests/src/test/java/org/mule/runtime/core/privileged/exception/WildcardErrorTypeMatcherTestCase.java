/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher.WILDCARD_TOKEN;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_TYPES;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.internal.error.DefaultErrorTypeBuilder;
import org.mule.runtime.core.api.exception.AbstractErrorTypeMatcherTestCase;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_TYPES)
public class WildcardErrorTypeMatcherTestCase extends AbstractErrorTypeMatcherTestCase {

  @Test
  public void anyMatchesAll() {
    ErrorType mockErrorType = mock(ErrorType.class);
    when(mockErrorType.getParentErrorType()).thenReturn(transformationErrorType);
    ErrorTypeMatcher anyMatcher = new WildcardErrorTypeMatcher(identifierFromErrorType(anyErrorType));

    assertThat(anyMatcher.match(anyErrorType), is(true));
    assertThat(anyMatcher.match(transformationErrorType), is(true));
    assertThat(anyMatcher.match(expressionErrorType), is(true));
    assertThat(anyMatcher.match(mockErrorType), is(true));
  }

  @Test
  public void matchEqual() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(identifierFromErrorType(transformationErrorType));

    assertThat(transformationMatcher.match(transformationErrorType), is(true));
  }

  @Test
  public void matchChild() {
    ComponentIdentifier customTransformerIdentifier =
        ComponentIdentifier.builder().name("custom").namespace(CORE_NAMESPACE_NAME).build();
    ErrorType customTransformerErrorType = DefaultErrorTypeBuilder.builder()
        .namespace(customTransformerIdentifier.getNamespace())
        .identifier(customTransformerIdentifier.getName())
        .parentErrorType(transformationErrorType)
        .build();

    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(identifierFromErrorType(transformationErrorType));

    assertThat(transformationMatcher.match(customTransformerErrorType), is(true));
  }

  @Test
  public void doesNotMatchParent() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(identifierFromErrorType(transformationErrorType));

    assertThat(transformationMatcher.match(anyErrorType), is(false));
  }

  @Test
  public void doesNotMatchSibling() {
    ErrorTypeMatcher transformationMatcher = new WildcardErrorTypeMatcher(identifierFromErrorType(transformationErrorType));

    assertThat(transformationMatcher.match(expressionErrorType), is(false));
  }

  @Test
  public void matchWildcardIdentifier() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(CORE_NAMESPACE_NAME + ":" + WILDCARD_TOKEN);
    ErrorType test1ErrorType = DefaultErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier("test1")
        .parentErrorType(transformationErrorType).build();
    ErrorType test2ErrorType = DefaultErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier("test2")
        .parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(test1ErrorType), is(true));
    assertThat(wildcardMatcher.match(test2ErrorType), is(true));
  }

  @Test
  public void wildcardIdentifierAlsoMatchesChildren() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(WILDCARD_TOKEN + ":TRANSFORMATION");
    ErrorType testErrorType =
        DefaultErrorTypeBuilder.builder().namespace("unknown").identifier("unknown").parentErrorType(transformationErrorType)
            .build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(true));
  }

  @Test
  public void wildcardNamespaceDoesntMatchesChildren() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(CORE_NAMESPACE_NAME + ":" + WILDCARD_TOKEN);
    ErrorType testErrorType =
        DefaultErrorTypeBuilder.builder().namespace("unknown").identifier("unknown").parentErrorType(transformationErrorType)
            .build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(false));
  }

  @Test
  public void matchWildcardNamespace() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(WILDCARD_TOKEN + ":fixedIdentifier");

    ErrorType test1ErrorType = DefaultErrorTypeBuilder.builder().namespace("test1").identifier("fixedIdentifier")
        .parentErrorType(transformationErrorType).build();
    ErrorType test2ErrorType = DefaultErrorTypeBuilder.builder().namespace("test2").identifier("fixedIdentifier")
        .parentErrorType(transformationErrorType).build();
    ErrorType test3ErrorType =
        DefaultErrorTypeBuilder.builder().namespace("test3").identifier("unknown").parentErrorType(transformationErrorType)
            .build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(test1ErrorType), is(true));
    assertThat(wildcardMatcher.match(test2ErrorType), is(true));
    assertThat(wildcardMatcher.match(test3ErrorType), is(false));
  }

  @Test
  public void doesntMatchOtherNamespaceIfTheWildcardIsOnIdentifier() {
    ComponentIdentifier wildcardErrorType =
        buildFromStringRepresentation("otherThan" + CORE_NAMESPACE_NAME + ":" + WILDCARD_TOKEN);

    ErrorType testErrorType = DefaultErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier("test")
        .parentErrorType(anyErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(false));
  }

  @Test
  public void doesntMatchOtherIdentifierIfTheWildcardIsOnNamespace() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(WILDCARD_TOKEN + ":anId");

    ErrorType testErrorType =
        DefaultErrorTypeBuilder.builder().namespace("test").identifier("otherId").parentErrorType(transformationErrorType)
            .build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);
    assertThat(wildcardMatcher.match(testErrorType), is(false));
  }

  @Test
  public void matchDoubleWildcard() {
    ComponentIdentifier wildcardErrorType = buildFromStringRepresentation(WILDCARD_TOKEN + ":" + WILDCARD_TOKEN);

    ErrorType test11ErrorType =
        DefaultErrorTypeBuilder.builder().namespace("ns1").identifier("id1").parentErrorType(transformationErrorType).build();
    ErrorType test12ErrorType =
        DefaultErrorTypeBuilder.builder().namespace("ns1").identifier("id2").parentErrorType(transformationErrorType).build();
    ErrorType test21ErrorType =
        DefaultErrorTypeBuilder.builder().namespace("ns2").identifier("id1").parentErrorType(transformationErrorType).build();
    ErrorType test22ErrorType =
        DefaultErrorTypeBuilder.builder().namespace("ns2").identifier("id2").parentErrorType(transformationErrorType).build();

    ErrorTypeMatcher wildcardMatcher = new WildcardErrorTypeMatcher(wildcardErrorType);

    assertThat(wildcardMatcher.match(test11ErrorType), is(true));
    assertThat(wildcardMatcher.match(test12ErrorType), is(true));
    assertThat(wildcardMatcher.match(test21ErrorType), is(true));
    assertThat(wildcardMatcher.match(test22ErrorType), is(true));
  }

  private static ComponentIdentifier identifierFromErrorType(ErrorType errorType) {
    return buildFromStringRepresentation(errorType.toString());
  }

}
