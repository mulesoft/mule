package org.mule;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NonBlockingFullySupportedFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-fully-supported-test-config.xml";
    }

    @Test
    public void defaultFlow() throws Exception
    {
        testFlowNonBlocking("defaultFlow");
    }

    @Test
    public void nonBlockingFlow() throws Exception
    {
        testFlowNonBlocking("nonBlockingFlow");
    }

    @Test
    public void subFlow() throws Exception
    {
        testFlowNonBlocking("subFlow");
    }

    @Test
    public void processorChain() throws Exception
    {
        testFlowNonBlocking("processorChain");
    }

    @Test
    public void filter() throws Exception
    {
        testFlowNonBlocking("filter");
    }

    @Test
    public void securityFilter() throws Exception
    {
        testFlowNonBlocking("security-filter");
    }

    @Test
    public void transformer() throws Exception
    {
        testFlowNonBlocking("transformer");
    }

    @Test
    public void choice() throws Exception
    {
        testFlowNonBlocking("choice");
    }

}

