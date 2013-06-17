/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.OAuth1Adapter;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth1UnauthorizeMessageProcessorTestCase
{

    @Mock
    private OAuth1Adapter adapter;
    private TestUnathorizeMessageProcessor processor;

    @Before
    public void setUp()
    {
        this.processor = new TestUnathorizeMessageProcessor();
        this.processor.setModuleObject(this.adapter);
    }

    @Test
    public void unathorize() throws Exception
    {
        this.processor.process(Mockito.mock(MuleEvent.class));
        Mockito.verify(this.adapter).reset();
    }

    private class TestUnathorizeMessageProcessor extends BaseOAuth1UnauthorizeMessageProcessor
    {

        @Override
        protected Class<? extends OAuth1Adapter> getAdapterClass()
        {
            return null;
        }
    }

}
