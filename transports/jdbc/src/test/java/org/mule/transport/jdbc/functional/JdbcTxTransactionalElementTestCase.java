/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEvent;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.DispatchException;
import org.mule.construct.Flow;

public class JdbcTxTransactionalElementTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcTxTransactionalElementTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-tx-transactional-element.xml"}
        });
    }
    
    @Before
    public void setUp() throws Exception
    {
        execSqlUpdate("delete from TEST");
    }

    @Test
    public void testTransactional() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactional");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
    }

    @Test
    public void testTransactionalFailInTheMiddle() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddle");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(0));
        assertThat(countWithType2,Is.is(0));
    }

    @Test
    public void testTransactionalFailAtEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAtEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(0));
        assertThat(countWithType2,Is.is(0));
    }

    @Test
    public void testTransactionalFailAfterEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAfterEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
    }

    @Test
    public void testTransactionalFailInTheMiddleWithCatchExceptionStrategy() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailInTheMiddleWithCatchExceptionStrategy");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(0));
    }

    @Test
    public void testTransactionalFailAtEndWithCatchExceptionStrategy() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalFailAtEndWithCatchExceptionStrategy");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
    }

    @Test
    public void testTransactionalDoesntFailWithAnotherResourceType() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalDoesntFailWithAnotherResourceType");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        Integer countWithType3 = getCountWithType3();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
        assertThat(countWithType3,Is.is(1));
    }

    @Test
    public void testTransactionalWithAnotherResourceTypeAndExceptionAtEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("transactionalWithAnotherResourceTypeAndExceptionAtEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        Integer countWithType3 = getCountWithType3();
        assertThat(countWithType1,Is.is(0));
        assertThat(countWithType2,Is.is(0));
        assertThat(countWithType3,Is.is(1));
    }

    @Test
    public void testNestedTransactional() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactional");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
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
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(0));
    }

    @Test
    public void testNestedTransactionalFailWithCatch() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalFailWithCatch");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
    }



    @Test
    public void testNestedTransactionalWithBeginOrJoin() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoin");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
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
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(0));
        assertThat(countWithType2,Is.is(0));
    }

    @Test
    public void testNestedTransactionalWithBeginOrJoinFailWithCatch() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nestedTransactionalWithBeginOrJoinFailWithCatch");
        MuleEvent event = getTestEvent("message", flow);
        flow.process(event);
        Integer countWithType1 = getCountWithType1();
        Integer countWithType2 = getCountWithType2();
        assertThat(countWithType1,Is.is(1));
        assertThat(countWithType2,Is.is(1));
    }
}
