/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.authentication;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.request.DefaultHttpAuthentication;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.Answers;

@SmallTest
public class BasicAuthenticationBuilderTestCase extends AbstractMuleTestCase
{

    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private static final String PREEMPTIVE = "preemptiveExpression";

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private MuleEvent mockMuleEvent = mock(MuleEvent.class, Answers.RETURNS_DEEP_STUBS.get());
    private BasicAuthenticationBuilder builder = new BasicAuthenticationBuilder(mockMuleContext);

    @Test
    public void basicConfig() throws MuleException
    {
        DefaultHttpAuthentication authentication = (DefaultHttpAuthentication) builder.setPassword(PASSWORD).setUsername(USERNAME).build();
        assertThat(authentication.getPassword(), is(PASSWORD));
        assertThat(authentication.getUsername(), is(USERNAME));
    }

    @Test
    public void basicConfigPreemptive() throws MuleException
    {
        DefaultHttpAuthentication authentication = (DefaultHttpAuthentication) builder.setPreemptive(true).build();
        assertThat(authentication.getPreemptive(), is("true"));
    }

    @Test
    public void basicConfigPreemptiveExpression() throws MuleException
    {
        DefaultHttpAuthentication authentication = (DefaultHttpAuthentication) builder.setPreemptiveExpression(PREEMPTIVE).build();
        assertThat(authentication.getPreemptive(), is(PREEMPTIVE));
    }

    @Test
    public void resolvesExpressionsCorrectly() throws MuleException
    {
        DefaultHttpAuthentication authentication = (DefaultHttpAuthentication) builder.setPassword(PASSWORD).setUsername(USERNAME).build();
        HttpRequestAuthentication requestAuthentication = authentication.resolveRequestAuthentication(mockMuleEvent);
        assertThat(requestAuthentication.getPassword(), is(PASSWORD));
        assertThat(requestAuthentication.getUsername(), is(USERNAME));
    }

}


