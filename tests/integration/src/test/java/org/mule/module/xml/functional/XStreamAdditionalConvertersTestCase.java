/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XStreamAdditionalConvertersTestCase extends AbstractServiceAndFlowTestCase
{
    private CountDownLatch latch = new CountDownLatch(1);

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/xstream-additional-converters-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/xstream-additional-converters-flow.xml"}});
    }

    public XStreamAdditionalConvertersTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("ObjectToXml");
        assertNotNull(testComponent);
        testComponent.setEventCallback(new Callback(latch));
    }

    @Test
    public void testAdditionalConverters() throws Exception
    {
        ISO8601DateConverter converter = new ISO8601DateConverter();
        String timestamp = converter.toString(new Date(System.currentTimeMillis()));
        String input = "<test-bean><createDate>" + timestamp + "</createDate></test-bean>";

        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://FromTest", input, null);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private static class Callback implements EventCallback
    {
        private CountDownLatch testLatch;

        public Callback(CountDownLatch latch)
        {
            testLatch = latch;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            Object payload = context.getMessage().getPayload();
            assertTrue(payload instanceof TestBean);
            assertNotNull(((TestBean) payload).getCreateDate());

            testLatch.countDown();
        }
    }

    public static class TestBean
    {
        private Date createDate = null;

        public Date getCreateDate()
        {
            return createDate;
        }

        public void setCreateDate(Date createDate)
        {
            this.createDate = createDate;
        }
    }
}
