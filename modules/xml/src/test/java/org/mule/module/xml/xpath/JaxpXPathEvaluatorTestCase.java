/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.xpath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.xml.util.XMLUtils.toDOMNode;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JaxpXPathEvaluatorTestCase extends AbstractMuleContextTestCase
{

    private static final int NUMBER_OF_THREADS = 10000;

    private CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
    private final JaxpXPathEvaluator jaxpXPathEvaluator = new SaxonXpathEvaluator();

    private MuleEvent evenEvent;
    private MuleEvent oddEvent;
    private Node evenNode;
    private Node oddNode;
    private Exception exception = null;

    @Before
    public void setUp() throws Exception
    {
        InputStream payload = getClass().getClassLoader().getResourceAsStream("test-concurrency-xpath-evaluator-even.xml");
        evenEvent = getTestEvent(payload);
        evenNode = toDOMNode(payload, evenEvent);
        payload = getClass().getClassLoader().getResourceAsStream("test-concurrency-xpath-evaluator-odd.xml");
        oddEvent = getTestEvent(payload);
        oddNode = toDOMNode(payload, oddEvent);
    }

    @Test
    public void testEvaluate() throws Exception
    {
        for (int i = 0; i < NUMBER_OF_THREADS; i++)
        {
            Runnable runner = new ConcurrencyTestRunner();
            Thread thread = new Thread(runner);
            thread.setName("Thread" + i);
            thread.start();
        }
        latch.await();
        assertThat(exception, nullValue());
    }

    private class ConcurrencyTestRunner implements Runnable
    {
        @Override
        public void run()
        {
            String threadName = Thread.currentThread().getName();
            int index = getIndex(threadName);
            try
            {
                verifyBandName(index);
                verifySongName(index);
            }
            catch (Exception e)
            {
                if(exception == null)
                {
                    exception = e;
                }
            }

            latch.countDown();
        }

    }


    private int getIndex(String name)
    {
        return Integer.parseInt(name.substring(6));
    }

    private void verifyBandName(int index)
    {
        NodeList nodeList ;
        if (index % 2 == 0)
        {
            nodeList = (NodeList) jaxpXPathEvaluator.evaluate("songs/song/band/text()", evenNode, XPathReturnType.NODESET, evenEvent);

            for (int i = 0; i < nodeList.getLength(); i++)
            {
                assertThat(nodeList.item(i).getNodeValue(), is("The Beatles"));
            }
        }
        else
        {
            nodeList = (NodeList) jaxpXPathEvaluator.evaluate("songs/song/band/text()", oddNode, XPathReturnType.NODESET, oddEvent);

            for (int i = 0; i < nodeList.getLength(); i++)
            {
                assertThat(nodeList.item(i).getNodeValue(), is("Oasis"));
            }
        }
    }

    private void verifySongName(int index)
    {
        NodeList nodeList ;
        if (index % 2 == 0)
        {
            nodeList = (NodeList) jaxpXPathEvaluator.evaluate("songs/song/name/text()", evenNode, XPathReturnType.NODESET, evenEvent);

            for (int i = 0; i < nodeList.getLength(); i++)
            {
                assertThat(nodeList.item(i).getNodeValue(), is("Song" + (i+1)));
            }
        }
        else
        {
            nodeList = (NodeList) jaxpXPathEvaluator.evaluate("songs/song/name/text()", oddNode, XPathReturnType.NODESET, oddEvent);

            for (int i = 0; i < nodeList.getLength(); i++)
            {
                assertThat(nodeList.item(i).getNodeValue(), is("Song" + (nodeList.getLength() - i)));
            }
        }
    }

}