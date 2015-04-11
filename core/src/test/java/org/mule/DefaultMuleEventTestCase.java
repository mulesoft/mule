/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;

import java.util.Map;

import org.junit.Test;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleTestCase
{

    public static final String CUSTOM_ENCODING = "UFT-8";
    public static final String PROPERTY_NAME = "test";
    public static final String PROPERTY_VALUE = "foo";

    private final MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private final DefaultMuleMessage muleMessage = new DefaultMuleMessage("test-data", (Map<String, Object>) null, muleContext);
    private final DefaultMuleEvent muleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.REQUEST_RESPONSE, (FlowConstruct) null);

    @Test
    public void setFlowVariableDefaultDataType() throws Exception
    {
        muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE);

        DataType<?> dataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void setFlowVariableCustomDataType() throws Exception
    {
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleEvent.setFlowVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType);

        DataType<?> actualDataType = muleEvent.getFlowVariableDataType(PROPERTY_NAME);
        assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
    }

    @Test
    public void setSessionVariableDefaultDataType() throws Exception
    {
        muleEvent.setSessionVariable(PROPERTY_NAME, PROPERTY_VALUE);

        DataType<?> dataType = muleEvent.getSessionVariableDataType(PROPERTY_NAME);
        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void setSessionVariableCustomDataType() throws Exception
    {
        DataType dataType = DataTypeFactory.create(String.class, APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);

        muleEvent.setSessionVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType);

        DataType<?> actualDataType = muleEvent.getSessionVariableDataType(PROPERTY_NAME);
        assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
    }
}