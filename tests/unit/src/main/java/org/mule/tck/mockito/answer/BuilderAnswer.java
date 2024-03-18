/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.mockito.answer;

import static org.mockito.Mockito.RETURNS_DEFAULTS;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


/**
 * Use this answer for mocking an object that implements the Builder Pattern. It guarantees the same mock is returned for every
 * method invocation which response type matches with the mock class.
 *
 * You could give it as parameter to {@link Mockito#mock(Class, Answer)}.
 */
public class BuilderAnswer implements Answer<Object> {

  public static final BuilderAnswer BUILDER_ANSWER = new BuilderAnswer();

  private BuilderAnswer() {}

  public Object answer(InvocationOnMock invocation) throws Throwable {
    Object mock = invocation.getMock();
    if (invocation.getMethod().getReturnType().isInstance(mock)) {
      return mock;
    } else {
      return RETURNS_DEFAULTS.answer(invocation);
    }
  }
}
