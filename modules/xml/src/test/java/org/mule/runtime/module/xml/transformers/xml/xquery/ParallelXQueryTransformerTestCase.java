/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml.xquery;

import static java.lang.Runtime.getRuntime;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XQueryTransformer;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class ParallelXQueryTransformerTestCase extends AbstractMuleContextTestCase {

  private static final int TIMEOUT_MILLIS = 30000;
  private String srcData;
  private String resultData;
  private ConcurrentLinkedQueue<Object> actualResults = new ConcurrentLinkedQueue<>();

  @Override
  protected void doSetUp() throws Exception {
    srcData = IOUtils.toString(IOUtils.getResourceAsStream("cd-catalog.xml", getClass()));
    resultData = IOUtils.toString(IOUtils.getResourceAsStream("cd-catalog-result.xml", getClass()));
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
  }

  public Transformer getTransformer() throws Exception {
    XQueryTransformer transformer = new XQueryTransformer();
    transformer.setReturnDataType(DataType.STRING);
    transformer.setXqueryFile("cd-catalog.xquery");
    transformer.setMuleContext(muleContext);
    transformer.initialise();
    return transformer;
  }

  private CountDownLatch latch = new CountDownLatch(getParallelThreadCount());

  public synchronized void signalDone() {
    latch.countDown();
  }

  @Test
  public void testParallelTransformation() throws Exception {
    final Transformer transformer = getTransformer();
    final Flow testFlow = new Flow(MuleTestUtils.APPLE_FLOW, muleContext);
    testFlow.setMessageProcessors(singletonList(new DefaultJavaComponent(new SingletonObjectFactory(muleContext))));

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < getParallelThreadCount(); ++i) {
      new Thread(() -> {
        try {
          setCurrentEvent(Event.builder(DefaultEventContext.create(testFlow, TEST_CONNECTOR))
              .message(InternalMessage.builder().payload("test").build()).exchangePattern(REQUEST_RESPONSE).flow(testFlow)
              .session(getTestSession(testFlow, muleContext)).build());
        } catch (Exception e1) {
          e1.printStackTrace();
          return;
        }

        for (int j = 0; j < getCallsPerThread(); ++j) {
          try {
            actualResults.add(transformer.transform(srcData));
          } catch (TransformerException e2) {
            actualResults.add(e2);
          }
        }
        signalDone();
      }).start();
    }

    assertTrue(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

    long endTime = System.currentTimeMillis();

    checkResult();

    if (logger.isDebugEnabled()) {
      logger.debug("Parallel transformations in " + getParallelThreadCount() + " threads with "
          + getCallsPerThread() + " calls/thread took " + (endTime - startTime) + " ms.");
    }
  }

  protected void checkResult() throws Exception {
    Object expectedResult = resultData;

    for (Object result : actualResults) {
      if (result instanceof Exception) {
        throw (Exception) result;
      }

      if (expectedResult != null && result instanceof String) {
        XMLAssert.assertXMLEqual((String) expectedResult, (String) result);
      } else {
        XMLAssert.assertEquals(expectedResult, result);
      }
    }
  }

  private int getParallelThreadCount() {
    return getRuntime().availableProcessors();
  }

  private int getCallsPerThread() {
    return 100;
  }
}
