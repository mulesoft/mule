/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;

public class ParallelXsltTransformerTestCase extends AbstractMuleContextTestCase
{
    private String srcData;
    private String resultData;
    private Collection<Object> actualResults = new ConcurrentLinkedQueue<Object>();

    @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass()), "UTF-8");
        resultData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-utf-8.html", getClass()),
            "UTF-8");
    }

    public Transformer getTransformer() throws Exception
    {
        XsltTransformer transformer = new XsltTransformer();
        transformer.setReturnDataType(DataTypeFactory.STRING);
        transformer.setXslFile("cdcatalog.xsl");
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        return transformer;
    }

    int running = 0;

    public synchronized void signalStarted()
    {
        ++running;
    }

    public synchronized void signalDone()
    {
        if (--running == 0) this.notify();
    }

    @Test
    public void testParallelTransformation() throws Exception
    {
        final Transformer transformer = getTransformer();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < getParallelThreadCount(); ++i)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    signalStarted();
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

        synchronized (this)
        {
            this.wait();
        }

        long endTime = System.currentTimeMillis();

        checkResult();

        if (logger.isDebugEnabled())
        {
            logger.debug("Parallel transformations in " + getParallelThreadCount() + " threads with "
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

            if (expectedResult instanceof String && result instanceof String)
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
        return 20;
    }

    private int getCallsPerThread()
    {
        return 100;
    }
}
