/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;

import java.util.List;
import java.util.Map;

public class FormTransformerTestCase extends AbstractMuleTestCase
{
    private FormTransformer transformer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        transformer = new FormTransformer();
    }

    public void testFormTransformer() throws TransformerException
    {
        DefaultMuleMessage msg = new DefaultMuleMessage("test1=value1&test2=value2&test3", muleContext);
        Object result = transformer.transform(msg);
        assertTrue(result instanceof Map);
        
        Map<String,String> m = (Map<String,String>) result;
        assertEquals("value1", m.get("test1"));
        assertEquals("value2", m.get("test2"));
        assertNull(m.get("test3"));
    }


    public void testMultipleValues() throws TransformerException
    {
        DefaultMuleMessage msg = new DefaultMuleMessage("test1=value1&test1=value2", muleContext);
        Object result = transformer.transform(msg);
        assertTrue(result instanceof Map);
        
        Map<String,Object> m = (Map<String,Object>) result;
        Object o = m.get("test1");
        assertTrue(o instanceof List);

        List list = (List) o;
        assertTrue(list.contains("value1"));
        assertTrue(list.contains("value2"));
        
    }

}
