package org.mule.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mule.module.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.module.http.api.HttpHeaders.Names.WWW_AUTHENTICATE;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.module.http.internal.request.ResponseValidator;
import org.mule.module.http.internal.request.ResponseValidatorException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HttpRequestWithNtlmShouldHaveVolatileSession extends AbstractNtlmTestCase
{

    @Override
    protected String getWorkstation()
    {
        return null;
    }

    @Override
    protected String getDomain()
    {
        return "";
    }

    /**
     * This test will verify that two request with NTLM-authentication can be made consequently.
     *
     * @return the flow name for the test defined {@link AbstractNtlmTestCase}.
     */
    @Override
    protected String getFlowName()
    {
        return "correctLoginNtlmFlow";
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-volatile-ntlm-auth-config.xml";
    }

    @Before
    public void setupNtlmAuthenticator()
    {
        setupTestAuthorizer(AUTHORIZATION, WWW_AUTHENTICATE, SC_UNAUTHORIZED);
    }

    @Ignore
    @Test(expected = ResponseValidatorException.class)
    public void httpRequestWithNtlmAuthAndDynamicPasswordFieldShouldHaveVolatileSession() throws Exception
    {
        runFlow("dynamicPasswordShouldUseVolatileNtlmSession");
    }
}
