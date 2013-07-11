/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.watermark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.config.spring.factories.WatermarkFactoryBean;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.polling.MessageProcessorPollingConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class WatermarkPollingTestCase extends FunctionalTestCase
{

    public static final String OS_KEY1 = "test1";
    public static final String OS_KEY2 = "test2";
    public static final String OS_KEY3 = "test3";
    public static final String OS_KEY4 = "test4";
    public static final String OS_KEY5 = "test5";
    public static final String OS_KEY6 = "test6";
    public static final String OS_KEY7 = "test7";
    public static final String PRE_EXISTENT_OS_VALUE = "testValue";
    public static final String DEFAULT_VALUE_WHEN_KEY_NOT_PRESENT = "noKey";
    public static final String MODIFIED_KEY_VALUE = "keyPresent";
    public static final String RESULT_OF_UPDATE_EXPRESSION = "valueUpdated";

    private Prober prober = new PollingProber(10000, 1000);

    private static List<String> foo = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/watermark/watermark-polling-config.xml";
    }


    @Before
    public void cleanFoo()
    {
        foo.clear();
    }

    /**
     * Scenario:
     * <p>
     * No Object store Defined.
     * No Update Expression defined
     * No Key present in the Object Store
     * </p>
     * <p/>
     * Result:
     * <p>
     * Executes the default value expression of watermark, registers it as a flow var, stores that value in the OS
     * at the end of the flow.
     * </p>
     */
    @Test
    public void pollWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("nameNotDefinedWatermarkObjectStoreFlow");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY1));
        assertEquals(DEFAULT_VALUE_WHEN_KEY_NOT_PRESENT, os.retrieve(OS_KEY1));

    }

    /**
     * Scenario:
     * <p>
     * No object store defined
     * No update expression defined.
     * No Object store Key present
     * The user changes the watermark value in the flow.
     * </p>
     * Result:
     * <p>
     * Executes the default value expression of watermark, registers it as a flow var, stores that value in the OS
     * at the end of the flow but The key is stored in the object store with the value that the user set in the flow
     * variable
     * </p>
     */
    @Test
    public void pollChangeKeyValueWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("changeWatermarkWihtNotDefinedWatermarkObjectStoreFlow");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY2));
        assertEquals(MODIFIED_KEY_VALUE, os.retrieve(OS_KEY2));
    }

    /**
     * Scenario:
     * <p>
     * No object store defined
     * No update expression defined.
     * The key is already present in the Object store
     * The user changes the watermark value in the flow.
     * </p>
     * Result:
     * <p>
     * Retrieves the key value from the Object store, registers it as a flow var, stores that value in the OS
     * at the end of the flow but The key is stored in the object store with the value that the user set in the flow
     * variable.
     * </p>
     * <p/>
     * Extra validation. The User uses the watermark value in the poll element.
     */
    @Test
    public void pollUsingWatermark() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY3, PRE_EXISTENT_OS_VALUE);
        executePollOf("usingWatermarkFlow");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY3));
        assertEquals(MODIFIED_KEY_VALUE, os.retrieve(OS_KEY3));
        assertTrue(foo.contains(PRE_EXISTENT_OS_VALUE));

    }

    /**
     * Scenario:
     * <p>
     * No object store defined
     * No update expression defined.
     * The key is already present in the Object store
     * The user changes the watermark value in the flow.
     * The specified Watermark key is an expression
     * </p>
     * Result:
     * <p>
     * Retrieves the key value from the Object store, registers it as a flow var, stores that value in the OS
     * at the end of the flow. The key expression is evaluated twice, at the beginning of the message source and at
     * the end of the flow
     * </p>
     */
    @Test
    public void watermarkWithKeyAsAnExpression() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY4, PRE_EXISTENT_OS_VALUE);
        executePollOf("watermarkWithKeyAsAnExpression");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY4));
        assertEquals(MODIFIED_KEY_VALUE, os.retrieve(OS_KEY4));
    }


    /**
     * Scenario:
     * <p>
     * No object store defined
     * The update expression is defined.
     * The key is already present in the Object store
     * </p>
     * Result:
     * <p/>
     * Retrieves the key value from the Object store, registers it as a flow var, stores that value in the OS
     * at the end of the flow but The key is stored in the object store with the result of the update expression specified
     * in watermark
     * <p/>
     */
    @Test
    public void watermarkWithUpdateExpression() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY5, PRE_EXISTENT_OS_VALUE);
        executePollOf("watermarkWithUpdateExpression");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY5));
        assertEquals(RESULT_OF_UPDATE_EXPRESSION, os.retrieve(OS_KEY5));
        assertTrue(foo.contains(RESULT_OF_UPDATE_EXPRESSION));
    }

    /**
     * Scenario:
     * <p>
     * No object store defined
     * The update expression is defined.
     * The key is already present in the Object store
     * The flow fails to execute
     * </p>
     * Result:
     * <p/>
     * The watermark is not updated
     * <p/>
     */
    @Test
    public void failingFlowWithWatermark() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY6, PRE_EXISTENT_OS_VALUE);
        executePollOf("failingFlowWithWatermark");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY6));
        assertEquals(PRE_EXISTENT_OS_VALUE, os.retrieve(OS_KEY6));
        assertFalse(foo.contains(RESULT_OF_UPDATE_EXPRESSION));
    }

    /**
     * Scenario:
     * <p>
     * No object store defined
     * The update expression is defined.
     * The key is already present in the Object store
     * The flow fails to execute but it is catched in a catch-exception-strategy
     * </p>
     * Result:
     * <p/>
     * The watermark is updated with the value that is set in the catch exception strategy
     * <p/>
     */
    @Test
    public void failingFlowWithCatchedExceptionWatermark() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY7, PRE_EXISTENT_OS_VALUE);
        executePollOf("failingFlowCachedExceptionWatermark");
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains(OS_KEY7));
        assertEquals("catchedException", os.retrieve(OS_KEY7));
        assertFalse(foo.contains(RESULT_OF_UPDATE_EXPRESSION));
    }


    /**
     * Scenario:
     * <p>
     * Watermark is configured in an async flow
     * </p>
     * Result:
     * <p/>
     * It fails the execution
     * <p/>
     */
    @Test
    public void watermarkWithAsyncProcessing() throws Exception
    {
        executePollOf("watermarkWithAsyncProcessing");
        assertFalse(foo.contains(RESULT_OF_UPDATE_EXPRESSION));
    }


    private ObjectStore getDefaultObjectStore()
    {
        ObjectStoreManager mgr = (ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER);
        return mgr.getObjectStore(WatermarkFactoryBean.MULE_WATERMARK_PARTITION);
    }

    private void executePollOf(String flowName) throws Exception
    {
        MessageProcessorPollingConnector connector = muleContext.getRegistry().lookupObject("connector.polling.mule.default");
        AbstractPollingMessageReceiver messageReceiver = (AbstractPollingMessageReceiver) connector.lookupReceiver(flowName + "~");
        messageReceiver.performPoll();
    }

    public static class FooComponent
    {

        public void process(String s)
        {
            synchronized (foo)
            {
                foo.add(s);
            }

        }
    }

    public static class PollStopper implements MuleContextAware
    {
        @Override
        public void setMuleContext(MuleContext context)
        {
            Map<String, MessageProcessorPollingConnector> connectors
                    = context.getRegistry().lookupByType(MessageProcessorPollingConnector.class);

            for (MessageProcessorPollingConnector connector : connectors.values())
            {
                connector.setInitialStateStopped(true);
            }
        }


    }
}