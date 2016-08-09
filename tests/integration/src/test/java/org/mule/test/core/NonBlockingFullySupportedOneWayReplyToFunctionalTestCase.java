/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Ignore("MULE-9792")
@RunWith(Parameterized.class)
public class NonBlockingFullySupportedOneWayReplyToFunctionalTestCase extends NonBlockingFullySupportedFunctionalTestCase {

  public NonBlockingFullySupportedOneWayReplyToFunctionalTestCase(ProcessingStrategy processingStrategy) {
    super(processingStrategy);
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{new DefaultFlowProcessingStrategy()}, {new NonBlockingProcessingStrategy()}});
  }

  @Override
  protected MessageExchangePattern getMessageExchnagePattern() {
    return MessageExchangePattern.ONE_WAY;
  }
}

