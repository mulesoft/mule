/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.properties;

import org.mule.impl.MuleMessage;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropsComponent implements Callable
{
    private static final Log logger = LogFactory.getLog(PropsComponent.class);

    protected static Apple testObjectProperty = new Apple();

    public Object onCall(UMOEventContext context) throws Exception
    {
        logger.debug("org.mule.test.usecases.props.PropsComponent");

        if ("component1".equals(context.getComponentDescriptor().getName()))
        {
            logger.debug("Adding: " + context.getComponentDescriptor().getName());
            Map props = new HashMap();
            props.put("stringParam", "param1");
            props.put("objectParam", testObjectProperty);
            UMOMessage msg = new MuleMessage(context.getMessageAsString(), props);
            logger.debug("Adding done: " + context.getComponentDescriptor().getName());
            return msg;
        }
        else
        {
            logger.debug("Verifying: " + context.getComponentDescriptor().getName());
            assertEquals("param1", context.getMessage().getProperty("stringParam"));
            assertEquals(testObjectProperty, context.getMessage().getProperty("objectParam"));
            logger.debug("Verifying done: " + context.getComponentDescriptor().getName());
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
