package org.mule.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractInvalidKeystoreTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    private Throwable exceptionFromSystemExceptionHandler;

    public AbstractInvalidKeystoreTestCase()
    {
        super();
    }

    @Override
    protected abstract String getConfigFile();

    @Test
    public void startingSslMessageReceiverWithoutKeystoreShouldThrowConnectException() throws Exception
    {
        try
        {
            muleContext.start();
        }
        catch (Exception e)
        {
            // Lastly caught by DefaultMuleContext, and raised as LifeCycleException
            assertThat(e, instanceOf(LifecycleException.class));

            // Since endpoint connection is done by a retry policy, the expected exception
            // comes inside a retry policy exhaustion exception
            Throwable retryPolicyException = e.getCause();
            assertThat(retryPolicyException, instanceOf(RetryPolicyExhaustedException.class));

            Throwable actualConnectException = retryPolicyException.getCause();
            assertThat(actualConnectException, instanceOf(ConnectException.class));

            assertThat(actualConnectException.getMessage(), containsString("tls-key-store"));

            exceptionFromSystemExceptionHandler = actualConnectException;
        }
        finally
        {
            // Fail if no exception was thrown
            assertThat(exceptionFromSystemExceptionHandler, notNullValue());
        }
    }
}
