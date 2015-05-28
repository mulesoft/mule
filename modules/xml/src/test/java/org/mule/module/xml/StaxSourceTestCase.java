/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.xml.stax.StaxSource;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXParseException;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StaxSourceTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private XMLStreamReader mockReader;

    @Mock
    private XMLStreamException mockException;

    @Before
    public void before() throws Exception
    {
        when(mockReader.getEventType()).thenReturn(1);
        when(mockReader.next()).thenThrow(mockException);
    }

    @Test
    public void parseExceptionWithoutLocation() throws Exception
    {
        parseWithException(-1, -1);
    }

    @Test
    public void parseExceptionWithLocation() throws Exception
    {
        Location location = mock(Location.class);
        final int columNumber = 10;
        final int lineNumber = 20;
        when(location.getColumnNumber()).thenReturn(columNumber);
        when(location.getLineNumber()).thenReturn(lineNumber);
        when(mockException.getLocation()).thenReturn(location);

        parseWithException(columNumber, lineNumber);
    }

    private void parseWithException(int expectedColumnNumber, int expectedLineNumber) throws Exception
    {
        StaxSource staxSource = new StaxSource(mockReader);

        try
        {
            staxSource.getXMLReader().parse("");
            fail("was expecting a exception");
        }
        catch (SAXParseException e)
        {
            assertThat(e.getColumnNumber(), is(expectedColumnNumber));
            assertThat(e.getLineNumber(), is(expectedLineNumber));
        }
    }

}
