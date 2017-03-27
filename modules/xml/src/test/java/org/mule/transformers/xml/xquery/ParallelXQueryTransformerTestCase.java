/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xquery;

import static java.lang.Runtime.getRuntime;
import static org.junit.Assert.assertTrue;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.MessageExchangePattern;
import org.mule.RequestContext;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.construct.Flow;
import org.mule.module.xml.transformer.XQueryTransformer;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.slf4j.Logger;

public class ParallelXQueryTransformerTestCase extends AbstractMuleContextTestCase
{
    private static final Logger LOGGER = getLogger(ParallelXQueryTransformerTestCase.class);
    private static final int TIMEOUT_MILLIS = 30000;
    private String srcData;
    private String resultData;
    private ConcurrentLinkedQueue<Object> actualResults = new ConcurrentLinkedQueue<>();

    @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.toString(IOUtils.getResourceAsStream("cd-catalog.xml", getClass()));
        resultData = IOUtils.toString(IOUtils.getResourceAsStream("cd-catalog-result.xml", getClass()));
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    public Transformer getTransformer() throws Exception
    {
        XQueryTransformer transformer = new XQueryTransformer();
        transformer.setReturnDataType(DataTypeFactory.STRING);
        transformer.setXqueryFile("cd-catalog.xquery");
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        return transformer;
    }

    private CountDownLatch latch = new CountDownLatch(getParallelThreadCount());

    public synchronized void signalDone()
    {
        latch.countDown();
    }

    @Test
    public void testParallelTransformation() throws Exception
    {
        final Transformer transformer = getTransformer();
        final Flow testFlow = getTestFlow(muleContext);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < getParallelThreadCount(); ++i)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        RequestContext.setEvent(MuleTestUtils.getTestEvent("test", testFlow, MessageExchangePattern.REQUEST_RESPONSE, muleContext));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        return;
                    }

                    for (int j = 0; j < getCallsPerThread(); ++j)
                    {
                        try
                        {
                            actualResults.add(transformer.transform(srcData));
                        }
                        catch (TransformerException e)
                        {
                            actualResults.add(e);
                        }
                    }
                    signalDone();
                }
            }).start();
        }

        assertTrue(latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));

        long endTime = System.currentTimeMillis();

        checkResult();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Parallel transformations in " + getParallelThreadCount() + " threads with "
                         + getCallsPerThread() + " calls/thread took " + (endTime - startTime) + " ms.");
        }
    }

    protected void checkResult() throws Exception
    {
        Object expectedResult = resultData;

        for (Object result : actualResults)
        {
            if (result instanceof Exception)
            {
                throw (Exception) result;
            }

            if (expectedResult != null && result instanceof String)
            {
                XMLAssert.assertXMLEqual((String) expectedResult, (String) result);
            }
            else
            {
                XMLAssert.assertEquals(expectedResult, result);
            }
        }
    }

    private int getParallelThreadCount()
    {
        return getRuntime().availableProcessors();
    }

    private int getCallsPerThread()
    {
        return 100;
    }
}
