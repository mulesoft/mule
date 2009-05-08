/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.filters;

import org.mule.DefaultMuleMessage;
import org.mule.tck.AbstractMuleTestCase;

public class IsJsonFilterTestCase extends AbstractMuleTestCase 
{

	private IsJsonFilter filter;

	protected void doSetUp() throws Exception 
	{
		filter = new IsJsonFilter();
        filter.setValidateParsing(true);
	}
	
	public void testFilterFalse() throws Exception
	{
		assertFalse(filter.accept(new DefaultMuleMessage("This is definitely not JSON.")));
	}

	public void testFilterFalse2() throws Exception
	{
		assertFalse(filter.accept(new DefaultMuleMessage("{name=\"This may be JSON\",bool:}")));
	}
	
	public void testFilterFalse3() throws Exception
	{
		assertFalse(filter.accept(new DefaultMuleMessage("[name=\"This may be JSON\",bool:]")));
	}
	
	public void testFilterTrue() throws Exception
	{
		assertTrue(filter.accept(new DefaultMuleMessage("{name=\"This is some nice JSON\",bool:true,int:1,\"id\":1,\"options\":[\"a\",\"f\"],\"doublev\":2.2}")));
	}
	
	public void testFilterTrue2() throws Exception
	{
		assertTrue(filter.accept(new DefaultMuleMessage("{name=\"This is some nice JSON\",bool:true,int:1,\"id\":1,\"options\":[\"a\",\"f\"],\"doublev\":2.2}")));
	}
	
	public void testFilterNull() throws Exception
	{
		assertFalse(filter.accept(new DefaultMuleMessage(null)));
	}

}
