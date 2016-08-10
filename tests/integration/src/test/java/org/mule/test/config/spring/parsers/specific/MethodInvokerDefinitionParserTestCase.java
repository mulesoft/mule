/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Test appears to be incorrectly written.")
public class MethodInvokerDefinitionParserTestCase extends AbstractIntegrationTestCase
{
    @Test
    public void testPojoFlow() throws Exception
    {
        assertEquals("start nullmethod2Arg1Arg2config2Val arg2Valmethod2Arg1Arg2config2Val ",
                flowRunner("pojoFlow").withPayload("start ").run().getMessageAsString());
        assertEquals("start nullmethod2Arg1Arg2null arg2Valmethod2Arg1Arg2null ",
                flowRunner("pojoFlow2").withPayload("start ").run().getMessageAsString());
    }

    @Override
    protected String getConfigFile()
    {
        return "pojo-invoke-test.xml";
    }
}
