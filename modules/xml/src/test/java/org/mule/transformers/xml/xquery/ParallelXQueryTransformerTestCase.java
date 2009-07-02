/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xquery;

import org.mule.RequestContext;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XQueryTransformer;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;

public class ParallelXQueryTransformerTestCase extends AbstractMuleTestCase
{
    private String srcData;
    private String resultData;
    private ConcurrentLinkedQueue actualResults = new ConcurrentLinkedQueue();

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
        transformer.setReturnClass(String.class);
        transformer.setXqueryFile("cd-catalog.xquery");
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

    public void testParallelTransformation() throws Exception
    {
        final Transformer transformer = getTransformer();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < getParallelThreadCount(); ++i)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        RequestContext.setEvent(getTestEvent("test"));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        return;
                    }

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

        for (Iterator it = actualResults.iterator(); it.hasNext();)
        {
            Object result = it.next();
            if (result instanceof Exception) throw (Exception) result;

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
        return 20;
    }

    private int getCallsPerThread()
    {
        return 100;
    }

}
