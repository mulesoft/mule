/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    protected String getConfigResources()
    {
        return "pojo-invoke-test.xml";
    }

}
