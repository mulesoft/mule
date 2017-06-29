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
public class HttpAuthenticationExceededRequestTimeoutTestCase extends FunctionalTestCase
{

    private static Integer TIMEOUT = 500;

    @Rule
    public SystemProperty timeoutSystemProperty = new SystemProperty("timeout", TIMEOUT.toString());

    @Rule
    public SystemProperty delayComponentSystemProperty = new SystemProperty("delayComponent", this.getClass().toString() + "$DelayComponent");

    @Rule
    public SystemProperty isPreemptiveSystemProperty;

    @Rule
    public DynamicPort port = new DynamicPort("port");


    public HttpAuthenticationExceededRequestTimeoutTestCase(String isPreemptive)
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
        return "http-exceeded-response-timeout-config.xml";
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
            Thread.sleep(TIMEOUT * 2);
            return eventContext.getMessage().getPayload();
        }
    }

}
