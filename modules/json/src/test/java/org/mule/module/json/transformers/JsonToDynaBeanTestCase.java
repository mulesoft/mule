/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.module.json.JsonData;
import org.mule.tck.AbstractMuleTestCase;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.beanutils.PropertyUtils;

public class JsonToDynaBeanTestCase extends AbstractMuleTestCase
{
	
	private final String json = "{name=\"json\",bool:true,int:1,double:2.2,func:function(a){ return a; },array:[1,2]}";
	private JsonData _bean;
	private JSONObject jsonObject;

	protected void doSetUp() throws Exception
	{
		jsonObject = (JSONObject)JSONSerializer.toJSON(this.json);
	}

	public void testTransform() throws Exception
	{
		JsonToObject transformer = new JsonToObject();
		transformer.setReturnClass(JsonData.class);

		_bean = (JsonData) transformer.transform(json);
		assertEquals(jsonObject.get("name"), PropertyUtils.getProperty(_bean, "name"));  
		assertEquals(jsonObject.get("bool"), PropertyUtils.getProperty(_bean, "bool"));  
		assertEquals(jsonObject.get("int"), PropertyUtils.getProperty(_bean, "int"));  
		assertEquals(jsonObject.get("double"), PropertyUtils.getProperty(_bean, "double"));  
		assertEquals(jsonObject.get("func"), PropertyUtils.getProperty(_bean, "func"));  
		Collection expected = JSONArray.toCollection(jsonObject.getJSONArray("array"));  
		assertEquals(expected, PropertyUtils.getProperty(_bean, "array"));  	
	}

	public void testTransformException()
	{
		try 
		{
			JsonToObject transformer = new JsonToObject();
			transformer.transform("0xdeadbeef");
			fail();
		} 
		catch (TransformerException tfe) 
		{
			// Expected
		}
	}

}
