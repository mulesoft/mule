/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MuleMessageProcessingManagerTestCase extends AbstractMuleTestCase {

  @Mock
  private MuleContext muleContext;

  @Mock
  private SystemExceptionHandler exceptionHandler;

  @Mock
  private PolicyManager policyManager;

  private MuleMessageProcessingManager processingManager;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getExceptionListener()).thenReturn(exceptionHandler);

    processingManager = new MuleMessageProcessingManager();
    processingManager.setMuleContext(muleContext);
    processingManager.setPolicyManager(policyManager);

    processingManager.initialise();
  }

  @Test
  public void exceptionHandlerInvokder() throws Exception {
    Exception e = new Exception();
    processingManager.phaseFailure(e);
    verify(exceptionHandler).handleException(e);
  }
}
