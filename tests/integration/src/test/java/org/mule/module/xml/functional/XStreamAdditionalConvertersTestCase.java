/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.functional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class XStreamAdditionalConvertersTestCase extends AbstractServiceAndFlowTestCase
{
    private CountDownLatch latch = new CountDownLatch(1);

    public XStreamAdditionalConvertersTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/xstream-additional-converters-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/xstream-additional-converters-flow.xml"}});
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
        String input = "<test-bean><createDate>2009-05-19T07:40:00</createDate></test-bean>";

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
