package org.mule.test.integration.security;

import static com.ning.http.client.AsyncHttpClientConfigDefaults.ASYNC_CLIENT;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpAuthenticationRequestTimeoutTestCase extends FunctionalTestCase
{

    private static Integer DELAY = 450;

    private static Integer TIMEOUT = 600;

    private static String GLOBAL_REQUEST_TIMEOUT = ASYNC_CLIENT + "requestTimeout";

    @Rule
    public SystemProperty globalRequestTimeoutSystemProperty= new SystemProperty(GLOBAL_REQUEST_TIMEOUT, "300");

    @Rule
    public SystemProperty timeoutSystemProperty = new SystemProperty("timeout", TIMEOUT.toString());

    @Rule
    public SystemProperty isPreemptiveSystemProperty;

    @Rule
    public DynamicPort port = new DynamicPort("port");


    public HttpAuthenticationRequestTimeoutTestCase(String isPreemptive)
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
    public void testExceededAuthenticationTimeout() throws Exception
    {
        MuleEvent event = runFlow("flowRequest");
        assertThat(event.getMessage().getPayloadAsString(), is("OK"));
    }

    public static class DelayComponent implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Thread.sleep(DELAY);
            return eventContext.getMessage().getPayload();
        }
    }

}
