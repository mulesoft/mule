/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.soap;

import org.mule.tck.NamedTestCase;

import javax.xml.rpc.ParameterMode;
import java.util.Iterator;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapMethodTestCase extends NamedTestCase
{
    public void testNamedParameterParsing() throws Exception {
        SoapMethod method = new SoapMethod("getPerson",
           "firstName:string:in, age:integer:in, returnClass:org.mule.providers.soap.Person");
        assertEquals("getPerson", method.getName());
        assertEquals(2, method.getNamedParameters().size());
        assertEquals(Person.class, method.getReturnClass());
        assertNull(method.getReturnType());

        NamedParameter[] nps = new NamedParameter[2];
        nps = (NamedParameter[])method.getNamedParameters().toArray(nps);
        Iterator i = method.getNamedParameters().iterator();
        NamedParameter np = nps[0];
        assertEquals("firstName", np.getName());
        assertEquals(NamedParameter.XSD_STRING, np.getType());
        assertEquals(ParameterMode.IN, np.getMode());

        np = nps[1];
        assertEquals("age", np.getName());
        assertEquals(NamedParameter.XSD_INTEGER, np.getType());
        assertEquals(ParameterMode.IN, np.getMode());
    }

    public void testNamedParameterParsing2() throws Exception {
        SoapMethod method = new SoapMethod("getAge",
           "firstName:string:inout,return:int");
        assertEquals("getAge", method.getName());
        assertEquals(1, method.getNamedParameters().size());
        assertNull(method.getReturnClass());
        assertEquals(NamedParameter.XSD_INT, method.getReturnType());

        Iterator i = method.getNamedParameters().iterator();
        NamedParameter np = (NamedParameter)i.next();
        assertEquals("firstName", np.getName());
        assertEquals(NamedParameter.XSD_STRING, np.getType());
        assertEquals(ParameterMode.INOUT, np.getMode());
    }

}
