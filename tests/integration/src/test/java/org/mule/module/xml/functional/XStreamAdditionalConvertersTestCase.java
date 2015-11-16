/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class XStreamAdditionalConvertersTestCase extends FunctionalTestCase
{
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xstream-additional-converters-flow.xml";
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

        MuleClient client = muleContext.getClient();
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
