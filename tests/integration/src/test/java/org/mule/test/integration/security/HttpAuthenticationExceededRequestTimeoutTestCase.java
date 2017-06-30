package org.mule.test.integration.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class HttpAuthenticationExceededRequestTimeoutTestCase extends AbstractHttpAuthenticationRequestTimeoutTestCase
{

    private static int TIMEOUT = 500;

    private static int DELAY = TIMEOUT * 2;

    public HttpAuthenticationExceededRequestTimeoutTestCase(String isPreemptive)
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

}
