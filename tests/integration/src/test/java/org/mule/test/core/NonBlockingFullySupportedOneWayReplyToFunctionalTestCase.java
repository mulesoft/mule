/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.Arrays.asList;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.ProcessingStrategyFactory;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class NonBlockingFullySupportedOneWayReplyToFunctionalTestCase extends NonBlockingFullySupportedFunctionalTestCase {

  public NonBlockingFullySupportedOneWayReplyToFunctionalTestCase(ProcessingStrategyFactory processingStrategyFactory) {
    super(processingStrategyFactory);
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {{new DefaultFlowProcessingStrategyFactory()}, {new NonBlockingProcessingStrategyFactory()}});
  }

  @Override
  protected MessageExchangePattern getMessageExchnagePattern() {
    return ONE_WAY;
  }
}

