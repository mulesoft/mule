/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import static com.google.common.net.MediaType.APPLICATION_BINARY;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.module.cxf.support.CxfUtils.resolveEncoding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

@RunWith(MockitoJUnitRunner.class)
public class CxfUtilsTestCase extends AbstractMuleTestCase
{
    @Mock
    private MuleMessage message;

    @Before
    public void setup()
    {
        when(message.getEncoding()).thenReturn(ISO_8859_1.displayName());
    }

    @Test
    public void resolveEncodingWhenPresentInContentType() throws Exception
    {
        assertThat(resolveEncoding(message, PLAIN_TEXT_UTF_8.toString()), equalToIgnoringCase(UTF_8.displayName()));
    }

    @Test
    public void resolveEncodingWhenNotPresentInContentType() throws Exception
    {
        assertThat(resolveEncoding(message, APPLICATION_BINARY.toString()), equalToIgnoringCase(ISO_8859_1.displayName()));
    }

}
