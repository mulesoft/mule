package org.mule.test.integration.providers.soap.axis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class TestAxisServiceUsingXFireEndpointTestCase extends FunctionalTestCase{

	protected static transient Log logger = LogFactory.getLog(TestAxisServiceUsingXFireEndpointTestCase.class);
	
	public TestAxisServiceUsingXFireEndpointTestCase(){
		super();
		this.setDisposeManagerPerSuite(true);
	}
	
	public void testAxis() throws Exception
	{
		MuleClient client = new MuleClient();
		UMOMessage reply = client.send("vm://axis.in", new MuleMessage("Test String"));
		
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(reply.getPayloadAsString(),"Received: Test String");
		logger.info(reply.getPayloadAsString());
	}
	
	public void testRequestWsdl() throws Exception
	{
		MuleClient client = new MuleClient();
		Map props = new HashMap();
		props.put("http.method", "GET");
		UMOMessage reply = client.send("http://localhost:81/services/AxisService?WSDL", "", props);
		
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		
		Document document = DocumentHelper.parseText(reply.getPayloadAsString());
		List nodes;

		nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
		assertEquals(((Element)nodes.get(0)).attribute("name").getStringValue(),"AxisService");
	}

	protected String getConfigResources() {
		
		return "org/mule/test/integration/providers/soap/axis/mule-config-axis-using-xfire.xml";
	}
}
