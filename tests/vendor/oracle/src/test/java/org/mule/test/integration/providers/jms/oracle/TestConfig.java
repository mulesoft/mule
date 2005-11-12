package org.mule.test.integration.providers.jms.oracle;

/**
 * Contains the settings used for integration testing.  
 * You need to set these according to your Oracle database.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public abstract class TestConfig {

	// Set these constants for your Oracle database.
	public static String DB_URL = "jdbc:oracle:oci:@eaid";
	public static String DB_USER = "eai";
	public static String DB_PASSWORD = "eai";

	// The following queues are created and dropped automatically by the integration
	// tests.  You only need to change them if these names will create a conflict for 
	// you.
	public static String QUEUE_RAW = "TEST_RAW";
	public static String QUEUE_TEXT = "TEST_TEXT";
    public static String QUEUE_XML = "TEST_XML";

    public static String TEXT_MESSAGE = "This is a text message.";
    public static String XML_MESSAGE = "<msg attrib=\"attribute\">This is an XML message.</msg>";
    public static String XML_MESSAGE_FILE = "message.xml"; //"org/mule/test/integration/providers/jms/oracle/message.xml";
}
