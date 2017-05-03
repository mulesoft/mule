/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.processor.ReferenceProcessor;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class GlobalInterceptingMessageProcessorsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "global-intercepting-mps-config.xml";
  }

  @Test
  public void testConfig() throws Exception {
    Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
    assertNotNull(flow1);
    List<Processor> mpList = flow1.getMessageProcessors();

    Processor mp2 = muleContext.getRegistry().lookupObject("messageFilter");
    assertTrue(mp2 instanceof MessageFilter);
    MessageFilter mf = (MessageFilter) mp2;
    assertTrue(mf.getFilter() instanceof WildcardFilter);
    assertFalse(mf.isThrowOnUnaccepted());
    assertMpPresent(mpList, mp2, MessageFilter.class);

    Processor mp4 = muleContext.getRegistry().lookupObject("combineCollectionsTransformer");
    assertTrue(mp4 instanceof CombineCollectionsTransformer);
    assertMpPresent(mpList, mp4, CombineCollectionsTransformer.class);
  }

  /**
   * Check that the list of message processors contains a duplicate of the MP looked up in the registry (ie. that the MP is a
   * prototype, not a singleton)
   */
  private void assertMpPresent(List<Processor> mpList, Processor mp, Class<?> clazz) {
    assertFalse(mpList.contains(mp));

    for (Processor theMp : mpList) {
      if (theMp instanceof ReferenceProcessor) {
        theMp = ((ReferenceProcessor) theMp).getReferencedProcessor();
      }
      if (clazz.isInstance(theMp)) {
        return;
      }
    }

    fail("No " + clazz.getSimpleName() + " found");
  }
}
