/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;

@SmallTest
public class HttpListenerTestCase extends AbstractMuleTestCase
{

    public static final String URI_PARAM_NAME = "uri-param-name";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private FlowConstruct mockFlowConstruct = mock(FlowConstruct.class, Answers.RETURNS_DEEP_STUBS.get());
    private DefaultHttpListenerConfig mockHttpListenerConfig = mock(DefaultHttpListenerConfig.class, Answers.RETURNS_DEEP_STUBS.get());
    private HttpListenerConnectionManager mockHttpListenerConnectionManager = mock(HttpListenerConnectionManager.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    public void cannotHaveTwoUriParamsWithSameName() throws Exception
    {
        final String listenerPath = String.format("/{%s}/{%s}", URI_PARAM_NAME, URI_PARAM_NAME);
        useInvalidPath(listenerPath);
    }

    @Test
    public void cannotHaveWildcardWithOtherCharacters() throws Exception
    {
        useInvalidPath("/path/*pepe");
    }

    private void useInvalidPath(String listenerPath) throws InitialisationException
    {

        final DefaultHttpListener httpListener = new DefaultHttpListener();
        httpListener.setMuleContext(mockMuleContext);
        httpListener.setFlowConstruct(mockFlowConstruct);
        httpListener.setConfig(mockHttpListenerConfig);
        when(mockHttpListenerConfig.resolvePath(anyString())).thenReturn(listenerPath);
        when(mockMuleContext.getRegistry().get(HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mockHttpListenerConnectionManager);
        httpListener.setPath(listenerPath);

        expectedException.expect(InitialisationException.class);
        httpListener.initialise();
    }

}
