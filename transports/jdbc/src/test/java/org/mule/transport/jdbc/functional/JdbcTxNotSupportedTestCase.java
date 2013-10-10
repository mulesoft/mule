/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleEvent;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.DispatchException;
import org.mule.construct.Flow;
import org.mule.context.notification.TransactionNotification;
import org.mule.util.concurrent.Latch;

public class JdbcTxNotSupportedTestCase extends AbstractJdbcFunctionalTestCase
{
    public JdbcTxNotSupportedTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-tx-not-supported-config.xml"}
        });
    }
    
    @Before
    public void setUp() throws Exception
    {
        execSqlUpdate("delete from TEST");
    }

    @Test
    public void testEndpointNotSupported() throws Exception
    {
        final Latch transactionCommitLatch = new Latch();
        muleContext.registerListener(new TransactionNotificationListener<TransactionNotification>() {
            @Override
            public void onNotification(TransactionNotification notification)
            {
                if (notification.getAction() == TransactionNotification.TRANSACTION_COMMITTED)
                {
                    transactionCommitLatch.release();
                }
            }
        });
        execSqlUpdate("INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES(1, NULL, NULL, NULL)");
        if (!(transactionCommitLatch.await(5, TimeUnit.SECONDS)))
        {
            fail("Transaction wasn't commited");    
        }
        assertThat(getCountWithType2(),Is.is(1));
        assertThat(getCountWithType3(),Is.is(1));

    }

    @Test
    public void testEndpointNotSupportedFailingAtEnd() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("endpointNotSupportedFailingAtEnd");
        MuleEvent event = getTestEvent("message", flow);
        try
        {
            flow.process(event);
        } catch (Exception e)
        {
        }
        Integer countWithType2 = getCountWithType2();
        Integer countWithType3 = getCountWithType3();
        assertThat(countWithType2,Is.is(1));
        assertThat(countWithType3,Is.is(1));
    }


}
