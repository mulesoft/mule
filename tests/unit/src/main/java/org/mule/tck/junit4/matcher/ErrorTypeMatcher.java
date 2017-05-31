/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsAnything;

/**
 * {@link Matcher} for {@link ErrorType} instances.
 *
 * @since 4.0
 */
public final class ErrorTypeMatcher extends TypeSafeMatcher<ErrorType> {

  private static Matcher anything = IsAnything.anything();
  private final Matcher<String> namespace;
  private final Matcher<String> type;

  private ErrorTypeMatcher(Matcher<String> namespace, Matcher<String> type) {
    this.namespace = namespace;
    this.type = type;
  }

  public static ErrorTypeMatcher errorType(ErrorTypeDefinition type) {
    return errorType(anything, is(type.getType()));
  }

  public static ErrorTypeMatcher errorType(String namespace, String type) {
    return new ErrorTypeMatcher(is(namespace), is(type));
  }

  public static ErrorTypeMatcher errorType(ComponentIdentifier errorIdentifier) {
    return new ErrorTypeMatcher(is(errorIdentifier.getNamespace()), is(errorIdentifier.getName()));
  }

  public static ErrorTypeMatcher errorType(Matcher<String> namespace, Matcher<String> type) {
    return new ErrorTypeMatcher(namespace, type);
  }

  @Override
  protected boolean matchesSafely(ErrorType item) {
    return type.matches(item.getIdentifier()) && namespace.matches(item.getNamespace());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an ErrorType with namespace: <");
    namespace.describeTo(description);
    description.appendText("> and type: <");
    type.describeTo(description);
    description.appendText(">");
  }

  @Override
  protected void describeMismatchSafely(ErrorType item, Description mismatchDescription) {
    mismatchDescription
        .appendText(format("was an ErrorType with namespace <%s> and type: <%s>", item.getNamespace(), item.getIdentifier()));
  }
}
