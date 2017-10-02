/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import org.mule.runtime.core.internal.exception.MessagingException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} for {@link MessagingException} instances.
 *
 * @since 4.0
 */
public class MessagingExceptionMatcher extends TypeSafeMatcher<MessagingException> {

  private EventMatcher eventMatcher;

  public MessagingExceptionMatcher(EventMatcher eventMatcher) {
    this.eventMatcher = eventMatcher;
  }

  public static MessagingExceptionMatcher withEventThat(EventMatcher eventMatcher) {
    return new MessagingExceptionMatcher(eventMatcher);
  }

  @Override
  protected boolean matchesSafely(MessagingException item) {
    return eventMatcher.matches(item.getEvent());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a MessagingException with ");
    eventMatcher.describeTo(description);
  }

}
