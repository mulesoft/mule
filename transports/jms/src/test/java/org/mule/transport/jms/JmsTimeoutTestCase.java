package org.mule.transport.jms;

import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.transport.DispatchException;
import org.mule.tck.junit4.FunctionalTestCase;

public class JmsTimeoutTestCase extends FunctionalTestCase
{
    private static final String PAYLOAD = "test";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected String getConfigFile()
    {
        return "jms-timeout-config.xml";
    }

    @Test
    public void validateTimeout() throws Exception
    {
        expectedException.expect(DispatchException.class);
        expectedException.expectCause(instanceOf(ResponseTimeoutException.class));
        runFlow("timeoutFlow", PAYLOAD);
    }
}
