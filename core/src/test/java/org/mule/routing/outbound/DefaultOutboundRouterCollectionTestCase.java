/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.component.simple.PassThroughComponent;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public class DefaultOutboundRouterCollectionTestCase extends AbstractMuleContextTestCase
{
    public DefaultOutboundRouterCollectionTestCase()
    {
        setStartContext(true);
    }

    private static int LATCH_AWAIT_TIMEOUT_MS = 1000;
    private Service testService;
    private MuleEvent testEvent;
    static MuleMessage originalMessage;
    private TestOutboundRouterCollection outboundRouter;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        testEvent = getTestEvent("TEST_MESSAGE", getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));
        testService = createService();
        outboundRouter = new TestOutboundRouterCollection();
        testService.setOutboundMessageProcessor(outboundRouter);
        outboundRouter.setMuleContext(muleContext);
        muleContext.getRegistry().registerService(testService);
    }

    protected Service createService() throws MuleException
    {
        SedaModel model = new SedaModel();
        muleContext.getRegistry().registerModel(model);
        Service service = new SedaService(muleContext);
        service.setName("test");
        service.setComponent(new PassThroughComponent());
        service.setModel(model);
        return service;
    }

    /**
     * If there is just one outbound router we don't need to do any copying at all
     * regardless of if matchAll is true or not or if the router mutates the message
     * in isMatch or not . The outbound phase already has a new message copy.
     * @throws Exception if the test fails!
     */
    @Test
    @Ignore("MULE-6926: Flaky test.")
    public void testSingleDoesNotRequireCopyRouterMatchAllFalse() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(false);
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(1);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there is just one outbound router we don't need to do any copying at all
     * regardless of if matchAll is true or not or if the router mutates the message
     * in isMatch or not . The outbound phase already has a new message copy.
     * @throws Exception if the test fails!
     */
    @Test
    public void testSingleDoesNotRequireCopyRouterMatchAllTrue() throws Exception
    {

        MuleEvent testEvent = getTestEvent("TEST_MESSAGE", MessageExchangePattern.ONE_WAY);
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(1);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there is just one outbound router we don't need to do any copying at all
     * regardless of if matchAll is true or not or if the router mutates the message
     * in isMatch or not . The outbound phase already has a new message copy.
     * @throws Exception if the test fails!
     */
    @Test
    public void testSingleRequiresCopyRouterMatchAllFalse() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(false);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(1);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there is just one outbound router we don't need to do any copying at all
     * regardless of if matchAll is true or not or if the router mutates the message
     * in isMatch or not . The outbound phase already has a new message copy.
     * @throws Exception if the test fails!
     */
    @Test
    public void testSingleRequiresCopyRouterMatchAllTrue() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(1);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    // MULTIPLE

    /**
     * If there are multiple outbound routers but matchAll is false then we only need
     * to copy message if the router might mutate it in isMatch, if not then no need
     * to copy.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleDoesNotRequireCopyRouterMatchAllFalse() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(false);
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there are multiple outbound routers and matchAll is true then we need a new
     * message copy for all but the *last* router independent of whether the routers
     * may mutate the message in isMatch or not. See MULE- 4352.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleDoesNotRequireCopyRouterMatchAllTrue() throws Exception
    {

        MuleEvent testEvent = getTestEvent("TEST_MESSAGE", MessageExchangePattern.ONE_WAY);
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there are multiple outbound routers and matchAll is false then we need a
     * new message copy for all but the *last* router that may mutate the message in
     * isMatch.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleRequiresCopyRouterMatchAllFalse() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(false);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If there are multiple outbound routers and matchAll is true then we need a new
     * message copy for all but the *last* router independent of whether the routers
     * may mutate the message in isMatch or not. See MULE- 4352.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleRequiresCopyRouterMatchAllTrue() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // MIX

    /**
     * If matchAll is true then we need a new message copy for each and every router except the last one.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleMixMatchAllTrue() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);
        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(2);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If matchAll is false then we need a new message copy for each router that may
     * mutate the message in isMatch unless it is the last router.
     * @throws Exception if the test fails!
     */
    @Test
    public void testMultipleMixMatchAllFalse() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(false);
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(true));
        getOutboundRouterCollection().addRoute(new TestDoesNotRequireNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);
        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    /**
     * If the message is a stream and message copying is required due to any of the
     * scenarios tested above then an exception should be thrown as the stream
     * payload cannot be copied.
     * @throws Exception if the test fails!
     */
    @Test
    public void testStreamPayload() throws Exception
    {
        getOutboundRouterCollection().setMatchAll(true);
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));
        getOutboundRouterCollection().addRoute(new TestRequiresNewMessageOutboundRouter(false));

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(2);

        testEvent = getTestEvent(new OutputHandler()
        {
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                // do nothing
            }
        }, getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE));
        try
        {
            testService.sendEvent(testEvent);
            fail("Exception was expected");
        }
        catch (MessagingException e)
        {
            // expected
        }
    }

    private OutboundRouterCollection getOutboundRouterCollection()
    {
        return (OutboundRouterCollection) testService.getOutboundMessageProcessor();
    }

    private static class TestRequiresNewMessageOutboundRouter extends OutboundPassThroughRouter
    {
        static CountDownLatch latch;
        private boolean expectCopy;

        public TestRequiresNewMessageOutboundRouter(boolean expectCopy)
        {
            this.expectCopy = expectCopy;
            List<Transformer> transformers = new ArrayList<Transformer>();
            transformers.add(new AbstractTransformer()
            {
                @Override
                public Object doTransform(Object src, String encoding) throws TransformerException
                {
                    return src;
                }
            });
            setTransformers(transformers);
        }

        @Override
        public List<MessageProcessor> getRoutes()
        {
            List<MessageProcessor> list = new ArrayList<MessageProcessor>();
            try
            {
                list.add(getTestOutboundEndpoint("out", "test://out"));
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
            return list;
        }

        @Override
        public boolean isMatch(MuleMessage message) throws MuleException
        {
            if (expectCopy)
            {
                assertNotSame(originalMessage, message);
            }
            else
            {
                assertSame(originalMessage, message);
            }
            latch.countDown();
            return false;
        }
    }

    private static class TestDoesNotRequireNewMessageOutboundRouter extends OutboundPassThroughRouter
    {
        static CountDownLatch latch;
        private boolean expectCopy;

        public TestDoesNotRequireNewMessageOutboundRouter(boolean expectCopy)
        {
            this.expectCopy = expectCopy;
        }

        @Override
        public List<MessageProcessor> getRoutes()
        {
            List<MessageProcessor> list = new ArrayList<MessageProcessor>();
            try
            {
                list.add(getTestOutboundEndpoint("out", "test://out"));
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
            return list;
        }

        @Override
        public boolean isMatch(MuleMessage message) throws MuleException
        {
            if (expectCopy)
            {
                assertNotSame(originalMessage, message);
            }
            else
            {
                assertSame(originalMessage, message);

            }
            latch.countDown();
            return false;
        }
    }

    private static class TestOutboundRouterCollection extends DefaultOutboundRouterCollection
    {
        public TestOutboundRouterCollection()
        {
            super();
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MessagingException
        {
            originalMessage = event.getMessage();
            return super.process(event);
        }
    }
}
