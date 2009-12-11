/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.api.transport.OutputHandler;
import org.mule.component.simple.PassThroughComponent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.NoActionTransformer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class DefaultOutboundRouterCollectionTestCase extends AbstractMuleTestCase
{
    private static int LATCH_AWAIT_TIMEOUT_MS = 1000;
    private Service testService;
    private MuleEvent testEvent;
    static MuleMessage originalMessage;
    private TestOutboundRouterCollection outboundRouter;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        testEvent = getTestInboundEvent("TEST_MESSAGE");
        testService = getTestService();
        testService.setComponent(new PassThroughComponent());
        outboundRouter = new TestOutboundRouterCollection();
        testService.setOutboundRouter(outboundRouter);
        outboundRouter.setMuleContext(muleContext);
    }

    /**
     * If there is just one outbound router we don't need to do any copying at all
     * regardless of if matchAll is true or not or if the router mutates the message
     * in isMatch or not . The outbound phase already has a new message copy.
     * @throws Exception if the test fails!
     */
    public void testSingleDoesNotRequireCopyRouterMatchAllFalse() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.start();

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
    public void testSingleDoesNotRequireCopyRouterMatchAllTrue() throws Exception
    {

        MuleEvent testEvent = getTestInboundEvent("TEST_MESSAGE");
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.start();

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
    public void testSingleRequiresCopyRouterMatchAllFalse() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

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
    public void testSingleRequiresCopyRouterMatchAllTrue() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

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
    public void testMultipleDoesNotRequireCopyRouterMatchAllFalse() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.start();

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
    public void testMultipleDoesNotRequireCopyRouterMatchAllTrue() throws Exception
    {

        MuleEvent testEvent = getTestInboundEvent("TEST_MESSAGE");
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.start();

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
    public void testMultipleRequiresCopyRouterMatchAllFalse() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

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
    public void testMultipleRequiresCopyRouterMatchAllTrue() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
    }

    // MIX

    /**
     * If matchAll is true then we need a new message copy for each and every router except the last one.
     * @throws Exception if the test fails!
     */
    public void testMultipleMixMatchAllTrue() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

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
    public void testMultipleMixMatchAllFalse() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

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
    public void testStreamPayload() throws Exception
    {
        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.start();

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(2);

        testEvent.getMessage().setPayload(new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                // do nothing
            }
        });
        MuleMessage result = testService.sendEvent(testEvent);
        assertTrue(result.getExceptionPayload() != null);
    }

    private static class TestRequiresNewMessageOutboundRouter extends OutboundPassThroughRouter
    {
        static CountDownLatch latch;
        private boolean expectCopy;

        public TestRequiresNewMessageOutboundRouter(boolean expectCopy)
        {
            this.expectCopy = expectCopy;
            List transformers = new ArrayList();
            transformers.add(new NoActionTransformer());
            setTransformers(transformers);
        }

        @Override
        public List getEndpoints()
        {
            List<OutboundEndpoint> list = new ArrayList<OutboundEndpoint>();
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
        public boolean isMatch(MuleMessage message) throws RoutingException
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
        public List getEndpoints()
        {
            List<OutboundEndpoint> list = new ArrayList<OutboundEndpoint>();
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
        public boolean isMatch(MuleMessage message) throws RoutingException
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

        @Override
        public boolean isRequiresNewMessage()
        {
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
        public MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException
        {
            originalMessage = message;
            return super.route(message, session);
        }
    }

}
