/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.properties;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropsComponent implements Callable
{
    private static final Log logger = LogFactory.getLog(PropsComponent.class);

    protected static Apple testObjectProperty = new Apple();

    public Object onCall(MuleEventContext context) throws Exception
    {
        logger.debug("org.mule.test.usecases.props.PropsComponent");

        if ("component1".equals(context.getFlowConstruct().getName()))
        {
            logger.debug("Adding: " + context.getFlowConstruct().getName());
            Map props = new HashMap();
            props.put("stringParam", "param1");
            props.put("objectParam", testObjectProperty);
            MuleMessage msg = new DefaultMuleMessage(context.getMessageAsString(), props, context.getMuleContext());
            logger.debug("Adding done: " + context.getFlowConstruct().getName());
            return msg;
        }
        else
        {
            logger.debug("Verifying: " + context.getFlowConstruct().getName());
            assertEquals("param1", context.getMessage().getOutboundProperty("stringParam"));
            assertEquals(testObjectProperty, context.getMessage().getOutboundProperty("objectParam"));
            logger.debug("Verifying done: " + context.getFlowConstruct().getName());
        }

        return context;
    }

    static protected void assertEquals(Object theObject, Object theProperty)
    {
        if (!theObject.equals(theProperty))
        {
            logger.error(String.valueOf(theObject) + " does not equal:" + String.valueOf(theProperty));
        }
        else
        {
            logger.debug("Woohoo!");
        }
    }

}
