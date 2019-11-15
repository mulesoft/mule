/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.listener.crl;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;
import org.mule.module.http.internal.listener.grizzly.GrizzlyServerManager;
import org.mule.module.http.internal.listener.grizzly.MuleSslFilter;

import java.io.IOException;
import java.util.Collection;

import javax.net.ssl.SSLHandshakeException;

import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(GrizzlyServerManager.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.crypto.*"})
public abstract class AbstractHttpListenerClrTestCase extends AbstractHttpTlsRevocationTestCase
{
    Throwable revocationException ;

    AbstractHttpListenerClrTestCase(String configFile, String crlPath, String entityCertified)
    {
        super(configFile, crlPath, entityCertified);
    }

    @Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {
                "http-listener-tls-revocation-file-config.xml",
                "http-listener-tls-revocation-crl-standard-config.xml"
        });
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        whenNew(MuleSslFilter.class).withAnyArguments().thenAnswer(new Answer<TestMuleSslFilter>()
        {
            @Override
            public TestMuleSslFilter answer(InvocationOnMock invocation) throws Throwable
            {
                return new TestMuleSslFilter((SSLEngineConfigurator) invocation.getArguments()[0], (SSLEngineConfigurator) invocation.getArguments()[1]);
            }
        });
    }

    void verifyRemotelyClosedCause(Exception e)
    {
        assertThat(e.getCause(), instanceOf(IOException.class));
        assertThat(e.getCause().getMessage(), containsString("Remotely closed"));
    }


    private class TestMuleSslFilter extends MuleSslFilter
    {

        TestMuleSslFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator)
        {
            super(serverSSLEngineConfigurator, clientSSLEngineConfigurator);
        }

        @Override
        protected void handleSSLException(SSLHandshakeException e)
        {
            revocationException = e;
            super.handleSSLException(e);
        }
    }
}
