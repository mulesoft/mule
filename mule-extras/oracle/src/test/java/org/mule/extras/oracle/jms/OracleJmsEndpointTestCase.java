package org.mule.extras.oracle.jms;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OracleJmsEndpointTestCase extends NamedTestCase
{
    public void testWithoutPayloadFactory() throws Exception {
    	UMOEndpointURI url = 
    		new MuleEndpointURI("jms://XML_QUEUE?transformers=XMLMessageToString");
        assertNull(url.getParams().getProperty(OracleJmsConnector.PAYLOADFACTORY_PROPERTY));
    }

    public void testWithPayloadFactory() throws Exception {
    	UMOEndpointURI url = 
    		new MuleEndpointURI("jms://XML_QUEUE" +
				"?" + OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "=oracle.xdb.XMLTypeFactory" +
	            "&transformers=XMLMessageToString");
        assertEquals("oracle.xdb.XMLTypeFactory", url.getParams().getProperty(OracleJmsConnector.PAYLOADFACTORY_PROPERTY));
    }
}
