
package org.mule.test.integration.xml;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import java.io.InputStream;

public class XmlSendTestCase extends FunctionalTestCase
{

    public void testXmlFilter() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:63081/xml-parse", xml, null);
        assertEquals("200", message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        
        // This won't pass the filter
        xml = getClass().getResourceAsStream("validation1.xml");
        message = client.send("http://localhost:63081/xml-parse", xml, null);
        assertEquals("406", message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    public void testXmlFilterAndXslt() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("request.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:63081/xml-xslt-parse", xml, null);
        assertEquals("200", message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }
    
    public void testXmlValidation() throws Exception
    {
        InputStream xml = getClass().getResourceAsStream("validation1.xml");

        assertNotNull(xml);

        MuleClient client = new MuleClient();

        // this will submit the xml via a POST request
        MuleMessage message = client.send("http://localhost:63081/validate", xml, null);
        assertEquals("200", message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        
        xml = getClass().getResourceAsStream("validation2.xml");
        message = client.send("http://localhost:63081/validate", xml, null);
        assertEquals("406", message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/xml/xml-conf.xml";
    }

}
