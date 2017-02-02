/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.HashSet;

import org.junit.Ignore;

import org.mule.compatibility.core.processor.simple.RemoveSessionVariableProcessor;
import org.mule.functional.transformer.simple.AbstractRemoveVariablePropertyProcessorTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleSession;
import org.mule.tck.size.SmallTest;

@SmallTest
@Ignore("MULE-9072 - Remove MuleSession")
public class RemoveSessionVariableProcessorTestCase extends AbstractRemoveVariablePropertyProcessorTestCase {

  public RemoveSessionVariableProcessorTestCase() {
    super(new RemoveSessionVariableProcessor());
  }

  @Override
  protected void addMockedPropeerties(Event mockEvent, HashSet properties) {
    MuleSession mockSession = mockEvent.getSession();
    when(mockSession.getPropertyNamesAsSet()).thenReturn(properties);
  }

  @Override
  protected void verifyRemoved(Event mockEvent, String key) {
    verify(mockEvent.getSession()).removeProperty(key);
  }

  @Override
  protected void verifyNotRemoved(Event mockEvent, String key) {
    verify(mockEvent.getSession(), times(0)).removeProperty(key);
  }
}
