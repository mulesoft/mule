/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.properties;

import java.util.HashMap;
import java.util.Map;

import org.mule.impl.MuleMessage;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;

public class PropsComponent implements Callable {

     protected static Apple testObjectProperty = new Apple();

     public Object onCall(UMOEventContext context) throws Exception
     {
         System.out.println("org.mule.test.usecases.props.PropsComponent");
         if("component1".equals(context.getComponentDescriptor().getName())) {
             System.out.println("Adding..." + context.getComponentDescriptor().getName());
             Map props = new HashMap();
             props.put("stringParam", "param1");
             props.put("objectParam",testObjectProperty);
             UMOMessage msg = new MuleMessage(context.getMessageAsString(), props);
             System.out.println("Adding done " + context.getComponentDescriptor().getName());
             return msg;
         } else {
             System.out.println("Verifying..." + context.getComponentDescriptor().getName());
             assertEquals("param1", context.getMessage().getProperty("stringParam"));
             assertEquals(testObjectProperty, context.getMessage().getProperty("objectParam"));
             System.out.println("Verifying done " + context.getComponentDescriptor().getName());
         }
         return context;
     }

     static protected void assertEquals(Object theObject, Object theProperty) {
         if (!theObject.equals(theProperty)) {
             System.out.println("ERROR:" + String.valueOf(theObject) + " does not equal:" + String.valueOf(theProperty));
         } else {
             System.out.println("Woohoo!");
         }
     }
}