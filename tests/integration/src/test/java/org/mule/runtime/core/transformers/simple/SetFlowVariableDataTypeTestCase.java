/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformers.simple;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MimeType;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;

import org.junit.Test;

public class SetFlowVariableDataTypeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "set-flow-variable-data-type-config.xml";
    }

    @Test
    public void setsPropertyDataType() throws Exception
    {
        final MuleEvent muleEvent = flowRunner("main").withPayload(TEST_MESSAGE).run();

        MuleMessage response = muleEvent.getMessage();
        DataType dataType = (DataType) response.getPayload();

        assertThat(dataType, DataTypeMatcher.like(String.class, MimeType.XML, UTF_16.name()));
    }

    public static class FlowVariableDataTypeAccessor implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            return eventContext.getEvent().getFlowVariableDataType("testVariable");
        }
    }
}
