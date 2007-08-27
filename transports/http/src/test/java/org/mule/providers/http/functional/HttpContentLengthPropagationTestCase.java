package org.mule.providers.http.functional;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.xml.XsltTransformer;
import org.mule.umo.UMOMessage;
import org.mule.util.IOUtils;

import java.io.InputStream;

public class HttpContentLengthPropagationTestCase extends FunctionalTestCase
{
    private static final String NAME_PAYLOAD = "test-xml-payload.xml";
    private static final String NAME_STYLESHEET = "stylesheet.xsl";

    public HttpContentLengthPropagationTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "http-content-length-propagation-conf.xml";
    }

    public void testContentLengthPropagation() throws Exception
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(NAME_PAYLOAD);
        assertNotNull("Payload test file not found.", is);
        byte[] fileContents = IOUtils.toByteArray(is);



        MuleClient client = new MuleClient();
        UMOMessage result = client.send("http://localhost:8085", new MuleMessage(fileContents));

        XsltTransformer trans = new XsltTransformer();
        trans.setXslFile(NAME_STYLESHEET);
        final byte[] locallyTransformedBytes = (byte[]) trans.doTransform(fileContents, "UTF-8");

        assertEquals(new String(locallyTransformedBytes), result.getPayloadAsString());

    }

}
