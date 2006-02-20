/*
=* AxisMessageStyleServiceTestCase.java
=*
=* Created on October 21, 2005, 1:11 PM
=*
=* To change this template, choose Tools | Options and locate the =emplate under
=* the Source Creation and Management node. Right-click the template and =hoose
=* Open. You can then make changes to the template in the Source Editor.
=*/
package org.mule.test.integration.providers.soap.axis.style;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import javax.xml.namespace.QName;

/**
 * @author Andrew Rutter
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
*/

public class AxisMessageStyleServiceTestCase extends FunctionalTestCase {
    private static String expectedResult = "TEST RESPONSE";

    public AxisMessageStyleServiceTestCase() {
        setDisposeManagerPerSuite(true);
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis/style/axis-mule-message-config.xml";
    }

    protected static String getServiceEndpoint() {
        return "http://localhost:8088/ServiceEntryPoint";
    }

    public void testDocument() throws Exception {

        String endpoint = getServiceEndpoint();
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName(new QName("document"));
        String ret = (String) call.invoke(new Object[]{expectedResult});
        assertNotNull(ret);
        assertEquals(expectedResult, ret);

        //Now try with the MuleClient
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("axis:" + getServiceEndpoint() + "?method=document", expectedResult, null);
        assertNotNull(result);
        assertEquals(expectedResult, result.getPayloadAsString());

    }

    public void testElementArray() throws Exception {

        String endpoint = getServiceEndpoint();
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName(new QName("elementArray"));
        String ret = (String) call.invoke(new Object[]{expectedResult});
        assertNotNull(ret);
        assertEquals(expectedResult, ret);

        //Now try with the MuleClient
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("axis:" + getServiceEndpoint() + "?method=elementArray", expectedResult, null);
        assertNotNull(result);
        assertEquals(expectedResult, result.getPayloadAsString());

    }

    public void testSoapBodyElement() throws Exception {

        String endpoint = getServiceEndpoint();
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName(new QName("soapBodyElement"));
        String ret = (String) call.invoke(new Object[]{expectedResult});
        assertNotNull(ret);
        assertEquals(expectedResult, ret);

        //Now try with the MuleClient
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("axis:" + getServiceEndpoint() + "?method=soapBodyElement", expectedResult, null);
        assertNotNull(result);
        assertEquals(expectedResult, result.getPayloadAsString());

    }

    public void testSoapRequestResponse() throws Exception {

        String endpoint = getServiceEndpoint();
        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress(new java.net.URL(endpoint));
        call.setOperationName(new QName("soapRequestResponse"));
        String ret = (String) call.invoke(new Object[]{expectedResult});
        assertNotNull(ret);
        assertEquals(expectedResult, ret);

        //todo this currently fails with a deserialisation error. need to fix
        //Now try with the MuleClient
//        MuleClient client = new MuleClient();
//
//        Map props = new HashMap();
//        props.put("doAutoTypes", "false");
//        UMOMessage result = client.send("axis:" + getServiceEndpoint() + "?method=soapRequestResponse", expectedResult, props);
//        assertNotNull(result);
//        assertEquals(expectedResult, result.getPayloadAsString());

    }
}

