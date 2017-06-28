package org.mule.test.integration.security;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpAuthenticationResponseTimeoutTestCase extends FunctionalTestCase
{

    private static Integer DELAY = 500;

    @Rule
    public SystemProperty delaySystemProperty = new SystemProperty("delay", DELAY.toString());

    @Rule
    public SystemProperty isPreemptiveSystemProperty;

    @Rule
    public DynamicPort port = new DynamicPort("port");


    public HttpAuthenticationResponseTimeoutTestCase(String isPreemptive)
    {
        this.isPreemptiveSystemProperty = new SystemProperty("isPreemptive", isPreemptive);
    }

    @Parameterized.Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {"true", "false"});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-response-timeout-config.xml";
    }

    @Test
    public void testAuthenticationTimeout() throws Exception
    {
        try
        {
            runFlow("flowRequest");
            fail("TimeoutException must be triggered");
        }
        catch (Exception timeoutException)
        {
            assertThat(timeoutException.getCause(), instanceOf(TimeoutException.class));
        }
    }

    public static class DelayComponent implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Thread.sleep(DELAY * 2);
            return eventContext.getMessage().getPayload();
        }
    }

}
