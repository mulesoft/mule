package org.mule.module.json.transformers;

import java.util.List;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.api.transformer.TransformerException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;

public class JsonToDynaBeanTestCase extends AbstractMuleTestCase
{
	
	private final String json = "{name=\"json\",bool:true,int:1,double:2.2,func:function(a){ return a; },array:[1,2]}";
	private DynaBean _bean;
	private JSONObject jsonObject;

	protected void doSetUp() throws Exception
	{
		jsonObject = (JSONObject)JSONSerializer.toJSON(this.json);
	}

	public void testTransform() throws Exception
	{
		JsonToObject transformer = new JsonToObject();
		transformer.setReturnClass(DynaBean.class);

		_bean = (DynaBean) transformer.transform(json);
		assertEquals(jsonObject.get("name"), PropertyUtils.getProperty(_bean, "name"));  
		assertEquals(jsonObject.get("bool"), PropertyUtils.getProperty(_bean, "bool"));  
		assertEquals(jsonObject.get("int"), PropertyUtils.getProperty(_bean, "int"));  
		assertEquals(jsonObject.get("double"), PropertyUtils.getProperty(_bean, "double"));  
		assertEquals(jsonObject.get("func"), PropertyUtils.getProperty(_bean, "func"));  
		List expected = JSONArray.toList(jsonObject.getJSONArray("array"));  
		assertEquals( expected, (List) PropertyUtils.getProperty(_bean, "array"));  	
	}

	public void testTransformException()
	{
		try 
		{
			JsonToObject transformer = new JsonToObject();
			transformer.transform("0xdeadbeef");
			fail();
		} catch (TransformerException tfe) {
			// Expected
		}
	}
}
