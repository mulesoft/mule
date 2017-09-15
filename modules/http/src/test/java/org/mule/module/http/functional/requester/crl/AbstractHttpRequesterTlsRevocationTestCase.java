/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester.crl;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
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

@RunWith(Parameterized.class)
public abstract class AbstractHttpRequesterTlsRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{

    AbstractHttpRequesterTlsRevocationTestCase(String configFile, String crlPath)
    {
        super(configFile, crlPath);
    }

    @Parameterized.Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {
                "http-requester-tls-revocation-file-config.xml",
                "http-requester-tls-revocation-crl-standard-config.xml"
        });
    }

}
