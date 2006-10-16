package org.mule.providers.rmi;


import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class RMIMultipleEndpointsTestCase extends FunctionalTestCase{
        
    public void testCase() throws Exception{        
        MuleClient client = new MuleClient();   
        
        //send Echo String
        UMOMessage message = client.send("vm://testin",Integer.valueOf(12),null);
        assertNotNull(message);
        Integer payload = (Integer)message.getPayload();
        assertEquals(payload,Integer.valueOf(22));
                
        //send String
        message = client.send("vm://testin","test matching component first time",null);
        assertNotNull(message);
        assertEquals((String)message.getPayload(),"emit tsrif tnenopmoc gnihctam tset");
        
        //send String
        message = client.send("vm://testin","test mathching component second time",null);
        assertNotNull(message);
        assertEquals((String)message.getPayload(),"emit dnoces tnenopmoc gnihchtam tset");
        
        //send Integer
        message = client.send("vm://testin",Integer.valueOf(15),null);
        assertNotNull(message);
        payload = (Integer)message.getPayload();
        assertEquals(payload,Integer.valueOf(25));        
    }

    protected String getConfigResources() {
        return "rmi-mule-config.xml";
    }
}
