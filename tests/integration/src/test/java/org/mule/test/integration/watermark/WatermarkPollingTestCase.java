/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.watermark;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;
import org.mule.api.schedule.Schedulers;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.module.springconfig.factories.WatermarkFactoryBean;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.store.ObjectStorePartition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class WatermarkPollingTestCase extends FunctionalTestCase
{

    private static final String OS_KEY1 = "test1";
    private static final String OS_KEY2 = "test2";
    private static final String OS_KEY3 = "test3";
    private static final String OS_KEY4 = "test4";
    private static final String OS_KEY5 = "test5";
    private static final String OS_KEY6 = "test6";
    private static final String OS_KEY7 = "test7";
    private static final String OS_KEY8 = "test8";
    private static final String PRE_EXISTENT_OS_VALUE = "testValue";
    private static final String DEFAULT_VALUE_WHEN_KEY_NOT_PRESENT = "noKey";
    private static final String MODIFIED_KEY_VALUE = "keyPresent";
    private static final String RESULT_OF_UPDATE_EXPRESSION = "valueUpdated";

    private final Prober prober = new PollingProber(3000, 500);

    private static final List<String> foo = new ArrayList<String>();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/watermark/watermark-polling-config.xml";
    }

    @Before
    public void cleanFoo()
    {
        foo.clear();
    }

    @Test
    public void testThatOsIsUserObjectStore()
    {
        ObjectStore<Serializable> defaultUserObjectStore = muleContext.getRegistry().lookupObject(
            "_defaultUserObjectStore");
        assertEquals(defaultUserObjectStore,
            ((ObjectStorePartition<Serializable>) getDefaultObjectStore()).getBaseStore());
    }

    /**
     * Scenario:
     * <p>
     * No Object store Defined. No Update Expression defined No Key present in the
     * Object Store
     * </p>
     * <p/>
     * Result:
     * <p>
     * Executes the default value expression of watermark, registers it as a flow
     * var, stores that value in the OS at the end of the flow.
     * </p>
     */
    @Test
    public void pollWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("nameNotDefinedWatermarkObjectStoreFlow");

        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY1)
                       && DEFAULT_VALUE_WHEN_KEY_NOT_PRESENT.equals(os.retrieve(OS_KEY1));
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY1;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined No update expression defined. No Object store Key
     * present The user changes the watermark value in the flow.
     * </p>
     * Result:
     * <p>
     * Executes the default value expression of watermark, registers it as a flow
     * var, stores that value in the OS at the end of the flow but The key is stored
     * in the object store with the value that the user set in the flow variable
     * </p>
     */
    @Test
    public void pollChangeKeyValueWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("changeWatermarkWihtNotDefinedWatermarkObjectStoreFlow");

        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY2) && MODIFIED_KEY_VALUE.equals(os.retrieve(OS_KEY2));
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY2;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined No update expression defined. The key is already
     * present in the Object store The user changes the watermark value in the flow.
     * </p>
     * Result:
     * <p>
     * Retrieves the key value from the Object store, registers it as a flow var,
     * stores that value in the OS at the end of the flow but The key is stored in
     * the object store with the value that the user set in the flow variable.
     * </p>
     * <p/>
     * Extra validation. The User uses the watermark value in the poll element.
     */
    @Test
    public void pollUsingWatermark() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY3, PRE_EXISTENT_OS_VALUE);
        executePollOf("usingWatermarkFlow");

        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY3) && MODIFIED_KEY_VALUE.equals(os.retrieve(OS_KEY3))
                       && foo.contains(PRE_EXISTENT_OS_VALUE);
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY3;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined No update expression defined. The key is already
     * present in the Object store The user changes the watermark value in the flow.
     * The specified Watermark key is an expression
     * </p>
     * Result:
     * <p>
     * Retrieves the key value from the Object store, registers it as a flow var,
     * stores that value in the OS at the end of the flow. The key expression is
     * evaluated twice, at the beginning of the message source and at the end of the
     * flow
     * </p>
     */
    @Test
    public void watermarkWithKeyAsAnExpression() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY4, PRE_EXISTENT_OS_VALUE);
        executePollOf("watermarkWithKeyAsAnExpression");
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY4) && MODIFIED_KEY_VALUE.equals(os.retrieve(OS_KEY4));
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY4;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined The update expression is defined. The key is already
     * present in the Object store
     * </p>
     * Result:
     * <p/>
     * Retrieves the key value from the Object store, registers it as a flow var,
     * stores that value in the OS at the end of the flow but The key is stored in
     * the object store with the result of the update expression specified in
     * watermark
     * <p/>
     */
    @Test
    public void watermarkWithUpdateExpression() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY5, PRE_EXISTENT_OS_VALUE);
        executePollOf("watermarkWithUpdateExpression");
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY5) && RESULT_OF_UPDATE_EXPRESSION.equals(os.retrieve(OS_KEY5))
                       && foo.contains(RESULT_OF_UPDATE_EXPRESSION);
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY5;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * Object store defined The update expression is defined. The key is already
     * present in the Object store The flow fails to execute
     * </p>
     * Result:
     * <p/>
     * The watermark is not updated
     * <p/>
     */
    @Test
    public void watermarkWithObjectStore() throws Exception
    {
        final ObjectStore<Serializable> os = muleContext.getRegistry().lookupObject(
            "_defaultInMemoryObjectStore");
        os.store(OS_KEY8, PRE_EXISTENT_OS_VALUE);
        executePollOf("watermarkWithObjectStore");

        prober.check(new ObjectStoreProbe(os)
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY8) && RESULT_OF_UPDATE_EXPRESSION.equals(os.retrieve(OS_KEY8))
                       && foo.contains(RESULT_OF_UPDATE_EXPRESSION);
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY8;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined The update expression is defined. The key is already
     * present in the Object store The flow fails to execute
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
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY6) && PRE_EXISTENT_OS_VALUE.equals(os.retrieve(OS_KEY6))
                       && !foo.contains(RESULT_OF_UPDATE_EXPRESSION);
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY6;
            }
        });
    }

    /**
     * Scenario:
     * <p>
     * No object store defined The update expression is defined. The key is already
     * present in the Object store The flow fails to execute but it is catched in a
     * catch-exception-strategy
     * </p>
     * Result:
     * <p/>
     * The watermark is updated with the value that is set in the catch exception
     * strategy
     * <p/>
     */
    @Test
    public void failingFlowWithCatchedExceptionWatermark() throws Exception
    {
        getDefaultObjectStore().store(OS_KEY7, PRE_EXISTENT_OS_VALUE);
        executePollOf("failingFlowCachedExceptionWatermark");
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(OS_KEY7) && "catchedException".equals(os.retrieve(OS_KEY7))
                       && !foo.contains(RESULT_OF_UPDATE_EXPRESSION);
            }

            @Override
            public String describeFailure()
            {
                return "The object store does not contain the key " + OS_KEY7;
            }
        });
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
    @Test(expected = AssertionError.class)
    public void watermarkWithAsyncProcessing() throws Exception
    {
        executePollOf("watermarkWithAsyncProcessing");

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return foo.contains(RESULT_OF_UPDATE_EXPRESSION);
            }

            @Override
            public String describeFailure()
            {
                return "The async mp was never called, which is what was expected";
            }
        });
    }

    @Test(expected = AssertionError.class)
    public void watermarkWithNullDefaultExpression() throws Exception
    {
        executePollOf("usingWatermarkFlowWithNullValue");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return !foo.isEmpty();
            }

            @Override
            public String describeFailure()
            {
                return "The foo collection is empty, which was expected";
            }
        });
    }

    @Test
    public void usingWatermarkFlowWithNullUpdateValue() throws Exception
    {
        executePollOf("usingWatermarkFlowWithNullUpdateValue");
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return foo.contains("defaultValue") && !os.contains("testUpdateAsNull");
            }

            @Override
            public String describeFailure()
            {
                return "The object store is storing null values";
            }
        });
    }

    @Test
    public void minSelectorWithList() throws Exception
    {
        this.assertVariableInOS("minSelectorWithList", new Character('A'), "The min value wasn't 'A'");
    }

    @Test
    public void maxSelectorWithList() throws Exception
    {
        this.assertVariableInOS("maxSelectorWithList", new Character('C'), "The max value wasn't 'C'");
    }

    @Test
    public void firstSelectorWithList() throws Exception
    {
        this.assertVariableInOS("firstSelectorWithList", "Apple", "The first value wasn't 'Apple'");
    }

    @Test
    public void lastSelectorWithList() throws Exception
    {
        this.assertVariableInOS("lastSelectorWithList", "Coconut", "The last value wasn't 'Coconut'");
    }

    @Test
    public void minSelectorWithIterator() throws Exception
    {
        this.assertVariableInOS("minSelectorWithIterator", new Character('A'), "The min value wasn't 'A'");
    }

    @Test
    public void maxSelectorWithIterator() throws Exception
    {
        this.assertVariableInOS("maxSelectorWithIterator", new Character('C'), "The max value wasn't 'C'");
    }

    @Test
    public void firstSelectorWithIterator() throws Exception
    {
        this.assertVariableInOS("firstSelectorWithIterator", "Apple", "The first value wasn't 'Apple'");
    }

    @Test
    public void lastSelectorWithIterator() throws Exception
    {
        this.assertVariableInOS("lastSelectorWithIterator", "Coconut", "The last value wasn't 'Coconut'");
    }

    private void assertVariableInOS(final String variableName,
                                    final Serializable expected,
                                    final String failureDescription) throws Exception
    {
        executePollOf(variableName);
        prober.check(new ObjectStoreProbe(getDefaultObjectStore())
        {
            @Override
            boolean evaluate(ObjectStore<Serializable> os) throws ObjectStoreException
            {
                return os.contains(variableName) && expected.equals(os.retrieve(variableName));
            }

            @Override
            public String describeFailure()
            {
                return failureDescription;
            }
        });
    }

    private ObjectStore<Serializable> getDefaultObjectStore()
    {
        ObjectStoreManager mgr = (ObjectStoreManager) muleContext.getRegistry().get(
            MuleProperties.OBJECT_STORE_MANAGER);
        return mgr.getObjectStore(WatermarkFactoryBean.MULE_WATERMARK_PARTITION);
    }

    private void executePollOf(String flowName) throws Exception
    {
        Collection<Scheduler> schedulers = muleContext.getRegistry().lookupScheduler(
            Schedulers.flowConstructPollingSchedulers(flowName));
        for (Scheduler scheduler : schedulers)
        {
            scheduler.schedule();
        }

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

    public static class PollStopper implements SchedulerFactoryPostProcessor
    {
        @Override
        public Scheduler process(Object job, final Scheduler scheduler)
        {
            return new Scheduler()
            {
                @Override
                public void schedule() throws Exception
                {
                    scheduler.schedule();
                }

                @Override
                public void dispose()
                {
                    scheduler.dispose();
                }

                @Override
                public void initialise() throws InitialisationException
                {
                    scheduler.initialise();
                }

                @Override
                public void setName(String name)
                {
                    scheduler.setName(name);
                }

                @Override
                public String getName()
                {
                    return scheduler.getName();
                }

                @Override
                public void start() throws MuleException
                {
                    // Do nothing
                }

                @Override
                public void stop() throws MuleException
                {
                    // do Nothing
                }
            };
        }
    }

    private abstract class ObjectStoreProbe implements Probe
    {

        private final ObjectStore<Serializable> os;

        public ObjectStoreProbe(ObjectStore<Serializable> os)
        {
            this.os = os;
        }

        @Override
        public boolean isSatisfied()
        {
            try
            {
                return evaluate(os);
            }
            catch (ObjectStoreException e)
            {
                return false;
            }
        }

        abstract boolean evaluate(ObjectStore<Serializable> defaultObjectStore) throws ObjectStoreException;

    }
}
