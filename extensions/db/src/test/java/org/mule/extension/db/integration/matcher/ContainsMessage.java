/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.matcher;

import org.mule.runtime.api.message.Message;

import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ContainsMessage extends TypeSafeMatcher<List<Message>> {

  private final String key;
  private final Object value;

  public ContainsMessage(String key, Object value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean matchesSafely(List<Message> messages) {
    for (Message message : messages) {
      if (message.getPayload().getValue() instanceof Map) {
        if (((Map) message.getPayload().getValue()).get(key).equals(value)) {
          return true;
        }
      }
    }
    return false;
  }

  public void describeTo(Description description) {
    description.appendText("Does not contains a map payload with key = " + key + " and value = " + value);
  }

  @Factory
  public static Matcher<List<Message>> mapPayloadWith(String key, Object value) {
    return new ContainsMessage(key, value);
  }

}
