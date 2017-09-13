/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.mule.construct.Flow;
import org.mule.module.http.internal.listener.grizzly.GrizzlyServerManager;
import org.mule.module.http.internal.listener.grizzly.MuleSslFilter;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.util.Collection;

import javax.net.ssl.SSLHandshakeException;

import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class HttpListenerTlsRevocationConfigTestCase extends FunctionalTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public DynamicPort portRevoked = new DynamicPort("portRevoked");

    private static final String REVOCATION_NOT_FOUND_ERROR_MESSAGE = "Could not determine revocation status";

    private static Throwable revocationException ;

    @Parameter
    public String configFile;

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

    @Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {
          //      "http-listener-tls-revocation-file-config.xml",
                "http-listener-tls-revocation-crl-standard-config.xml"
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void clientCertifiedAndRevokedWithCrlFile() throws Exception
    {
        revocationException = null;
        Flow flow = (Flow) getFlowConstruct("testFlowClientRevoked");
        try
        {
            flow.process(getTestEvent("data"));
        }
        catch (Exception e)
        {
            verifyRemotelyClosedCause(e);
        }
        finally
        {
            assertThat(revocationException, is(not(nullValue())));
            assertThat(revocationException.getMessage(), is(not(REVOCATION_NOT_FOUND_ERROR_MESSAGE)));
        }
    }

    private void verifyRemotelyClosedCause(Exception e)
    {
        assertThat(e.getCause(), instanceOf(IOException.class));
        assertThat(e.getCause().getMessage(), containsString("Remotely closed"));
    }

    private class TestMuleSslFilter extends MuleSslFilter
    {

        public TestMuleSslFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator)
        {
            super(serverSSLEngineConfigurator, clientSSLEngineConfigurator);
        }

        @Override
        protected void handleSSLException(SSLHandshakeException e)
        {
            Throwable rootCause = getRootCause(e);
            if(rootCause instanceof CertPathValidatorException)
            {
                revocationException = rootCause;
            }
            super.handleSSLException(e);
        }
    }

}
