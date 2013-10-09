/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.filters;

import org.mule.DefaultMuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IsJsonFilterTestCase extends AbstractMuleContextTestCase
{

    private IsJsonFilter filter;

    protected void doSetUp() throws Exception 
    {
        filter = new IsJsonFilter();
        filter.setValidateParsing(true);
    }
    
    @Test
    public void testFilterFalse() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage("This is definitely not JSON.", muleContext)));
    }

    @Test
    public void testFilterFalse2() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage("{name=\"This may be JSON\",bool:}", muleContext)));
    }
    
    @Test
    public void testFilterFalse3() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage("[name=\"This may be JSON\",bool:]", muleContext)));
    }
    
    @Test
    public void testFilterTrue() throws Exception
    {
        assertTrue(filter.accept(new DefaultMuleMessage("{\n" +
                "        \"in_reply_to_user_id\":null,\n" +
                "        \"text\":\"test from Mule: 6ffca02b-9d52-475e-8b17-946acdb01492\"}", muleContext)));
    }
    
    @Test
    public void testFilterNull() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage(null, muleContext)));
    }

    @Test
    public void testFilterWithObject() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage(new Object(), muleContext)));
    }

}
