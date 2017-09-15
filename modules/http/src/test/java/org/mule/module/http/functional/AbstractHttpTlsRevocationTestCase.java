/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static java.security.cert.CRLReason.KEY_COMPROMISE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateRevokedException;

import org.junit.Rule;

public abstract class AbstractHttpTlsRevocationTestCase extends FunctionalTestCase
{

    private static final String UNDETERMINED_REVOCATION_ERROR_MESSAGE = "Could not determine revocation status";

    protected static final String EMPTY_CRL_FILE_PATH = "src/test/resources/tls/crl/emptyCrl";

    protected static final String REVOKED_CRL_FILE_PATH = "src/test/resources/tls/crl/validCrl";

    protected static final String OUTDATED_CRL_FILE_PATH = "src/test/resources/tls/crl/outdatedCrl";

    protected static String ENTITY_CERTIFIED_1_SUB_PATH = "entity1";

    protected static String ENTITY_CERTIFIED_2_SUB_PATH = "entity2";

    protected static String ENTITY_CERTIFIED_3_SUB_PATH = "entity3";

    @Rule
    public DynamicPort portRevoked = new DynamicPort("port");

    @Rule
    public SystemProperty crlSystemProperty ;

    @Rule
    public SystemProperty entityCertifiedSubPathSystemProperty ;

    public String configFile;


    public AbstractHttpTlsRevocationTestCase(String configFile, String crlPath, String entityCertified)
    {
        this.configFile = configFile;
        crlSystemProperty = new SystemProperty("crlPath", crlPath);
        entityCertifiedSubPathSystemProperty = new SystemProperty("entityCertifiedSubPath", entityCertified);
    }


    @Override
    protected String getConfigFile()
    {
        return configFile;
    }


    protected MuleEvent runRevocationTestFlow () throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowRevoked");
        return flow.process(getTestEvent("data"));
    }

    protected void verifyUndeterminedRevocationException (Throwable e)
    {
        Throwable rootException = getRootCause(e);
        assertThat(rootException, is(instanceOf(CertPathValidatorException.class)));
        assertThat(rootException.getMessage(),is(UNDETERMINED_REVOCATION_ERROR_MESSAGE));
    }

    protected void verifyRevocationException(Throwable e)
    {
        Throwable rootException = getRootCause(e);
        assertThat(rootException, is(instanceOf(CertificateRevokedException.class)));
        assertThat(((CertificateRevokedException)rootException).getRevocationReason(), is(KEY_COMPROMISE));
    }

    protected void verifyNotRevokedEntity () throws Exception
    {
        MuleEvent result = runRevocationTestFlow();
        assertThat(result.getMessage().getPayloadAsString(), is("OK"));
    }

}
