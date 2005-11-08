package org.mule.test.integration.providers.jms.oracle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

/** 
 * Ignore "ignorable whitespace" when comparing XMLs. 
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public abstract class AbstractXmlTestCase extends XMLTestCase {
    
	/** Ignore "ignorable whitespace" when comparing XMLs. */
    public void setUp() throws Exception {  
    	super.setUp();    	
    	XMLUnit.setIgnoreWhitespace(true);
    }

    private static Log log = LogFactory.getLog(AbstractXmlTestCase.class);
}
