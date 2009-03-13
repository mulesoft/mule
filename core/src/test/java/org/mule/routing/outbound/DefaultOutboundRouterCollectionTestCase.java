
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
    private static MuleMessage originalMessage;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        testEvent = getTestInboundEvent("TEST_MESSAGE");
        testService = getTestService();
        testService.setComponent(new PassThroughComponent());
        testService.setOutboundRouter(new TestOutboundRouterCollection());
    }

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

    public void testMultipleDoesNotRequireCopyRouterMatchAllTrue() throws Exception
    {

        MuleEvent testEvent = getTestInboundEvent("TEST_MESSAGE");

        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.start();

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));

    }

    public void testMultipleRequiresCopyRouterMatchAllFalse() throws Exception
    {

        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.start();

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestRequiresNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));

    }

    public void testMultipleRequiresCopyRouterMatchAllTrue() throws Exception
    {

        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.start();

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));

    }

    // MIX

    public void testMultipleMixMatchAllTrue() throws Exception
    {

        testService.getOutboundRouter().setMatchAll(true);
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.start();

        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(3);
        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(2);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));

    }

    public void testMultipleMixMatchAllFalse() throws Exception
    {

        testService.getOutboundRouter().setMatchAll(false);
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.getOutboundRouter().addRouter(new TestRequiresNewMessageOutboundRouter(true));
        testService.getOutboundRouter().addRouter(new TestDoesNotRequireNewMessageOutboundRouter(false));
        testService.start();

        TestDoesNotRequireNewMessageOutboundRouter.latch = new CountDownLatch(3);
        TestRequiresNewMessageOutboundRouter.latch = new CountDownLatch(2);

        testService.sendEvent(testEvent);

        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));
        assertTrue(TestDoesNotRequireNewMessageOutboundRouter.latch.await(LATCH_AWAIT_TIMEOUT_MS,
            TimeUnit.MILLISECONDS));

    }

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
        }

        @Override
        public List getEndpoints()
        {
            List list = new ArrayList<OutboundEndpoint>();
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
            return true;
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
            List list = new ArrayList<OutboundEndpoint>();
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

    private class TestOutboundRouterCollection extends DefaultOutboundRouterCollection
    {
        @Override
        public MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException
        {
            originalMessage = message;
            return super.route(message, session);
        }
    }

}
