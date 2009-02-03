
package org.mule.module.xml.filters;

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import junit.framework.TestCase;
import org.mule.DefaultMuleMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;

/**
 * @author Ryan Heaton
 */
public class XPathFilterTestCase extends TestCase
{

    /**
     * tests accepting the mule message.
     */
    public void testAcceptMessage() throws Exception
    {
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        DefaultMuleMessage message = new DefaultMuleMessage(null);
        XPathFilter filter = new XPathFilter()
        {
            @Override
            public Node toDOMNode(Object src) throws Exception
            {
                return document;
            }

            @Override
            protected boolean accept(Node node)
            {
                return node == document;
            }
        };

        assertFalse("shouldn't accept a message if no payload is set.", filter.accept(message));
        message.setPayload(new Object());
        filter.setPattern("/some/pattern = null");
        assertTrue(filter.accept(message));
        assertEquals("null", filter.getExpectedValue());
        assertEquals("/some/pattern", filter.getPattern().trim());
        assertSame(document, message.getPayload());
        message.setPayload(new Object());
        filter.setExpectedValue(null);
        assertTrue(filter.accept(message));
        assertEquals("true", filter.getExpectedValue());
        assertEquals("/some/pattern", filter.getPattern().trim());
        assertSame(document, message.getPayload());
    }

    /**
     * tests accepting a node.
     */
    public void testAcceptNode() throws Exception
    {
        InputStream testXml = getClass().getResourceAsStream("/test.xml");
        assertNotNull(testXml);
        XPathFilter filter = new XPathFilter();

        // todo: test fails if using intel's xpath evaluator.
        // comment this out to demonstrate.
        filter.setXpath(new XPathFactoryImpl().newXPath());

        filter.setPattern("/some/unknown/path");
        filter.setExpectedValue("bogus");
        filter.initialise();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document document = builderFactory.newDocumentBuilder().parse(testXml);
        assertFalse("shouldn't have accepted a null evaluation when expected value isn't null.",
            filter.accept(document));
        filter.setExpectedValue("null");
        assertTrue(filter.accept(document));
        filter.setPattern("test/some/in");
        assertFalse(filter.accept(document));
        filter.setExpectedValue("another");
        assertTrue(filter.accept(document));

    }

    /**
     * tests accepting a node.
     */
    public void testAcceptSoapNode() throws Exception
    {
        InputStream soapEnvelope = getClass().getResourceAsStream("/request.xml");
        assertNotNull(soapEnvelope);
        XPathFilter filter = new XPathFilter();

        filter.setXpath(new XPathFactoryImpl().newXPath());

        filter.setPattern("/soap:Envelope/soap:Body/mule:echo/mule:echo");
        filter.setExpectedValue("Hello!");
        HashMap<String, String> prefix2Namespace = new HashMap<String, String>();
        prefix2Namespace.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        prefix2Namespace.put("mule", "http://simple.component.mule.org/");
        filter.setNamespaces(prefix2Namespace);
        filter.initialise();
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document envDoc = builderFactory.newDocumentBuilder().parse(soapEnvelope);
        assertTrue(filter.accept(envDoc));
    }

}
