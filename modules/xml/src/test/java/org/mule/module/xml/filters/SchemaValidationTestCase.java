
package org.mule.module.xml.filters;

import org.mule.DefaultMuleMessage;
import org.mule.module.xml.filters.SchemaValidationFilter;

import junit.framework.TestCase;


/**
 * @author Ryan Heaton
 */
public class SchemaValidationTestCase extends TestCase
{

    /**
     * tests validation
     */
    public void testValidate() throws Exception
    {
        SchemaValidationFilter filter = new SchemaValidationFilter();
        filter.setSchemaFile("schema1.xsd");
        filter.initialise();
        
        assertTrue(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation1.xml"))));
        assertFalse(filter.accept(new DefaultMuleMessage(getClass().getResourceAsStream("/validation2.xml"))));
    }

}
