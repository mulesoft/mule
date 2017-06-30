package org.mule.test.integration.security;

import static java.util.Arrays.asList;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractHttpAuthenticationRequestTimeoutTestCase extends FunctionalTestCase
{

    private static Integer timeout;

    private static Integer delay;

    @Rule
    public SystemProperty timeoutSystemProperty;

    @Rule
    public SystemProperty isPreemptiveSystemProperty;

    @Rule
    public DynamicPort port = new DynamicPort("port");


    public AbstractHttpAuthenticationRequestTimeoutTestCase(String isPreemptive)
    {
        this.timeout = getTimeout();
        this.delay = getDelay();
        this.timeoutSystemProperty = new SystemProperty("timeout", timeout.toString());
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

    public static class DelayComponent implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Thread.sleep(delay);
            return eventContext.getMessage().getPayload();
        }
    }

    protected abstract int getTimeout();

    protected abstract int getDelay();

}
