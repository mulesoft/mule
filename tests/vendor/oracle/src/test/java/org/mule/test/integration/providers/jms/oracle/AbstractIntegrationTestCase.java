package org.mule.test.integration.providers.jms.oracle;

import oracle.jms.AQjmsSession;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.mule.config.ConfigurationException;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.Message;
import org.mule.extras.client.MuleClient;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.umo.manager.UMOManager;

public abstract class AbstractIntegrationTestCase extends XMLTestCase {

    // The UMOManager is a singleton so we only reinitialize it when the server
    // configuration has changed.
    protected static UMOManager manager = null;
    protected static String configurationFiles = null;

    // Reference variables to the running Mule server's configuration.
    protected static OracleJmsConnector jmsConnector = null;
    protected static AQjmsSession jmsSession = null;

    protected static MuleClient muleClient;

    public AbstractIntegrationTestCase() {
        super();
        // Ignore "ignorable whitespace" when comparing XMLs.
        XMLUnit.setIgnoreWhitespace(true);
    }

    abstract protected String getConfigurationFiles();

    protected void setUp() throws Exception {
        super.setUp();

        // The UMOManager is a singleton so we only reinitialize it when the server
        // configuration has changed.
        if ((manager == null) ||
            (getConfigurationFiles().equals(configurationFiles) == false)){

            if (manager != null) {
                //if (manager.isStarted()) manager.stop();
                manager.dispose();
            }
            configurationFiles = getConfigurationFiles();
            manager = new MuleXmlConfigurationBuilder().configure(configurationFiles);
            manager.start();

            // Get these reference variables once the Mule server has started.
            jmsConnector = ((OracleJmsConnector) manager.lookupConnector("oracleJmsConnector"));
            if (jmsConnector == null) {
                throw new ConfigurationException(Message.createStaticMessage("Unable to lookup the Oracle JMS Connector."));
            }
            jmsSession = (AQjmsSession) jmsConnector.getSession(false, false);

            // Only initialize the client once (after server startup).
            muleClient = new MuleClient();
        }
    }
}
