/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration.transactions.local;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.jms.JMSException;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.DispatchException;
import org.mule.construct.Flow;
import org.mule.transport.jms.integration.AbstractJmsFunctionalTestCase;

public class JmsSingleTransactionTransactionalElementTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "integration/transactions/local/jms-single-tx-transactional.xml";
    }

    @Before
    public void setUpTest() throws JMSException
    {
        purge("out1");
        purge("out2");
        purge("out3");
    }
    
    @Test
    public void testTransactional() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactional");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testTransactionalFailInTheMiddle() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddle");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        }
        catch (Exception e) {}
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testTransactionalFailAtEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAtEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        }
        catch (Exception e) {}
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testTransactionalFailAfterEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAfterEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        }
        catch (Exception e) {}
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testTransactionalFailInTheMiddleWithCatchExceptionStrategy() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddleWithCatchExceptionStrategy");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testTransactionalFailAtEndWithCatchExceptionStrategy() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAtEndWithCatchExceptionStrategy");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testTransactionalFailsWithAnotherResourceType() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailsWithAnotherResourceType");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
            fail("DispatchException should be thrown");
        }
        catch (DispatchException e)
        {
        }
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testTransactionalDoesntFailWithAnotherResourceType() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalDoesntFailWithAnotherResourceType");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        MuleMessage message3 = muleContext.getClient().request("out3", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
        assertThat(message3, notNullValue());
    }

    @Test
    public void testTransactionalWithAnotherResourceTypeAndExceptionAtEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalWithAnotherResourceTypeAndExceptionAtEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        }
        catch (Exception e) {}
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        MuleMessage message3 = muleContext.getClient().request("out3", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
        assertThat(message3, notNullValue());
    }

@Test
    public void testNestedTransactional() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactional");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testNestedTransactionalFail() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalFail");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testNestedTransactionalFailWithCatch() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalFailWithCatch");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }



    @Test
    public void testNestedTransactionalWithBeginOrJoin() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoin");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testNestedTransactionalWithBeginOrJoinFail() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFail");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
    }

    @Test
    public void testNestedTransactionalWithBeginOrJoinFailWithCatch() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatch");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, notNullValue());
        assertThat(message2, notNullValue());
    }

    @Test
    public void testNestedTransactionalWithBeginOrJoinFailWithCatchAndRollback() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatchAndRollback");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        MuleMessage message1 = muleContext.getClient().request("out1", 1000);
        MuleMessage message2 = muleContext.getClient().request("out2", 1000);
        assertThat(message1, nullValue());
        assertThat(message2, nullValue());
    }

}
