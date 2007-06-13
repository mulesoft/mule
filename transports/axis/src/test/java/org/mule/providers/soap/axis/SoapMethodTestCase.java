/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

/**
 * 
 */
public class SoapMethodTestCase extends AbstractMuleTestCase
{
    public void testNamedParameterParsing() throws Exception
    {
        SoapMethod method = new SoapMethod("getFruit",
            "firstName;string;in, age;integer;in, returnClass;org.mule.tck.testmodels.fruit.Apple");
        assertEquals(new QName("getFruit"), method.getName());
        assertEquals(2, method.getNamedParameters().size());
        assertEquals(Apple.class, method.getReturnClass());
        assertNull(method.getReturnType());

        Iterator i = method.getNamedParameters().iterator();
        NamedParameter np = (NamedParameter)i.next();
        assertEquals(new QName("firstName"), np.getName());
        assertEquals(NamedParameter.XSD_STRING, np.getType());
        assertEquals(ParameterMode.IN, np.getMode());

        np = (NamedParameter)i.next();
        assertEquals(new QName("age"), np.getName());
        assertEquals(NamedParameter.XSD_INTEGER, np.getType());
        assertEquals(ParameterMode.IN, np.getMode());
    }

    public void testNamedParameterParsing2() throws Exception
    {
        SoapMethod method = new SoapMethod("getAge", "firstName;string;inout,return;int");
        assertEquals(new QName("getAge"), method.getName());
        assertEquals(1, method.getNamedParameters().size());
        assertEquals(Object.class, method.getReturnClass());
        assertEquals(NamedParameter.XSD_INT, method.getReturnType());

        Iterator i = method.getNamedParameters().iterator();
        NamedParameter np = (NamedParameter)i.next();
        assertEquals(new QName("firstName"), np.getName());
        assertEquals(NamedParameter.XSD_STRING, np.getType());
        assertEquals(ParameterMode.INOUT, np.getMode());
    }

}
