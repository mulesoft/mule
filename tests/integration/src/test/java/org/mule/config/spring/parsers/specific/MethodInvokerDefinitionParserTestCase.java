/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

@Ignore("Test appears to be incorrectly written.")
public class MethodInvokerDefinitionParserTestCase extends FunctionalTestCase
{
    @Test
    public void testPojoFlow() throws Exception
    {
        Flow flow = muleContext.getRegistry().lookupObject("pojoFlow");
        Flow flow2 = muleContext.getRegistry().lookupObject("pojoFlow2");

        assertEquals("start nullmethod2Arg1Arg2config2Val arg2Valmethod2Arg1Arg2config2Val ", flow.process(
            getTestEvent("start ")).getMessageAsString());

        assertEquals("start nullmethod2Arg1Arg2null arg2Valmethod2Arg1Arg2null ", flow2.process(
            getTestEvent("start ")).getMessageAsString());
    }

    @Override
    protected String getConfigFile()
    {
        return "pojo-invoke-test.xml";
    }
}
