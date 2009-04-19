package org.mule.module.json.filters;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.DefaultMuleMessage;
import org.mule.module.json.filters.IsJsonFilter;

public class IsJsonFilterTestCase extends AbstractMuleTestCase 
{

	private IsJsonFilter filter;

	protected void doSetUp() throws Exception 
	{
		filter = new IsJsonFilter();
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
