package org.mule.test.integration.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class HttpAuthenticationResponseTimeoutTestCase extends FunctionalTestCase
{

    private static Integer DELAY = 500;

    @Rule
    public SystemProperty delaySystemProperty = new SystemProperty("delay", DELAY.toString());

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-response-timeout-config.xml";
    }

    @Test
    public void testPreemptiveTimeout() throws Exception
    {
        try
        {
            runFlow("flowRequestPreemptive");
            fail("TimeoutException must be triggered");
        }
        catch(Exception timeoutException)
        {
            assertThat(timeoutException.getCause(), instanceOf(TimeoutException.class));
        }
    }

    @Test
    public void testNonPreemptiveTimeout() throws Exception
    {
        try
        {
            runFlow("flowRequestNonPreemptive");
            fail("TimeoutException must be triggered");
        }
        catch(Exception timeoutException)
        {
            assertThat(timeoutException.getCause(), instanceOf(TimeoutException.class));
        }
    }

    public static class DelayComponent implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Thread.sleep(DELAY + 500);
            return eventContext.getMessage().getPayload();
        }
    }
}
