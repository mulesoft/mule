package org.mule.test.integration.providers.jms.oracle;


/**
 * Makes sure the XMLUnit library is properly configured.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class SanityTestCase extends AbstractXmlTestCase {
    
    public void testSanity() throws Exception {
        assertXMLEqual(TestConfig.XML_MESSAGE, TestConfig.XML_MESSAGE);
    }
}
