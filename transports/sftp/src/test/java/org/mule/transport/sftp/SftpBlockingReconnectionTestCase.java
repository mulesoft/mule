/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;


import static org.hamcrest.core.Is.isA;

import org.mule.retry.RetryPolicyExhaustedException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SftpBlockingReconnectionTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void muleContextCreationFails() throws Exception
    {
        expectedException.expectCause(isA(RetryPolicyExhaustedException.class));
        new ApplicationContextBuilder().setApplicationResources(new String[]{"mule-sftp-blocking-reconnect-config.xml"}).build();
    }
}
