/*
 * $Id: ComponentDefinitionParserTestCase.java 19191 2010-08-25 21:05:23Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.construct.SimpleFlowConstruct;
import org.mule.tck.FunctionalTestCase;

public class MethodInvokerDefinitionParserTestCase extends FunctionalTestCase
{

    public void testPojoFlow() throws Exception
    {
        SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("pojoFlow");
        SimpleFlowConstruct flow2 = muleContext.getRegistry().lookupObject("pojoFlow2");

        assertEquals("start method2Arg1config1Val arg2Valmethod2Arg1Arg2config2Val ", flow.process(
            getTestEvent("start ")).getMessageAsString());

        assertEquals("start method2Arg1config1Val arg2Valmethod2Arg1Arg2null ", flow2.process(
            getTestEvent("start ")).getMessageAsString());

    }

    @Override
    protected String getConfigResources()
    {
        return "pojo-invoke-test.xml";
    }

}
