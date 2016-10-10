/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.mule.tck.MuleTestUtils.processAsStreamAndBlock;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * TODO
 */
@RunWith(Parameterized.class)
public abstract class AbstractReactiveProcessorTestCase extends AbstractMuleContextTestCase {

  private boolean reactive;

  public AbstractReactiveProcessorTestCase(boolean reactive) {
    this.reactive = reactive;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{false}, {true}});
  }

  @Override
  protected Event process(Processor processor, Event event) throws Exception {
    if (reactive) {
      try {
        return processAsStreamAndBlock(event, processor);
      } catch (MessagingException msgException) {
        // unwrap MessageingException to ensure same exception is thrown by blocking and non-blocking processing
        throw msgException.getCause() != null ? (Exception) msgException.getCause() : msgException;
      }
    } else {
      return processor.process(event);
    }
  }

  protected boolean isReactive() {
    return this.reactive;
  }
}
