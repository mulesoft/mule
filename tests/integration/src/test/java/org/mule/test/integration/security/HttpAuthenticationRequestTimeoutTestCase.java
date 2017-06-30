package org.mule.test.integration.security;

import static com.ning.http.client.AsyncHttpClientConfigDefaults.ASYNC_CLIENT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpAuthenticationRequestTimeoutTestCase extends AbstractHttpAuthenticationRequestTimeoutTestCase
{

    private static String GLOBAL_TIMEOUT = "300";

    private static int TIMEOUT = 600;

    private static int DELAY = 450;

    private static String GLOBAL_REQUEST_TIMEOUT = ASYNC_CLIENT + "requestTimeout";

    @Rule
    public SystemProperty globalRequestTimeoutSystemProperty= new SystemProperty(GLOBAL_REQUEST_TIMEOUT, GLOBAL_TIMEOUT);

    public HttpAuthenticationRequestTimeoutTestCase(String isPreemptive)
    {
        super(isPreemptive);
    }

    @Override
    protected int getTimeout()
    {
        return TIMEOUT;
    }

    @Override
    protected int getDelay()
    {
        return DELAY;
    }

    @Test
    public void testExceededAuthenticationTimeout() throws Exception
    {
        MuleEvent event = runFlow("flowRequest");
        assertThat(event.getMessage().getPayloadAsString(), is("OK"));
    }

}
