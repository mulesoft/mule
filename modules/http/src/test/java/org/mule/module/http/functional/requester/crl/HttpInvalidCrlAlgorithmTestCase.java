/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.crl;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.security.tls.TlsConfiguration.formatInvalidCrlAlgorithm;


public class HttpInvalidCrlAlgorithmTestCase extends AbstractMuleTestCase
{

    private MuleContext context;

    @Before
    public void before() throws InitialisationException, ConfigurationException
    {
        context = new DefaultMuleContextFactory().createMuleContext();
    }

    @After
    public void after()
    {
        if (context != null)
        {
            context.dispose();
        }
    }

    @Test
    public void testInvalidCrlAlgorithm() throws ConfigurationException
    {
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("http-requester-tls-crl-invalid-algorithm-config.xml");
        try
        {
            builder.configure(context);
        }
        catch (ConfigurationException e)
        {
            Throwable rootCause = getRootCause(e);
            assertThat(rootCause, instanceOf(CreateException.class));
            assertThat(rootCause.getMessage(), is(formatInvalidCrlAlgorithm("SunX509")));
        }
    }

}
