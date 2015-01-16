/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.spring.config;

import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;

import java.io.IOException;
import java.util.Map;
import java.util.ServiceLoader;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 */
public class UnsupportedConnectorsMuleArtifactTestCase
{
    @Test
    public void unsupportedConnectors() throws SAXException, IOException, MuleArtifactFactoryException
    {
        //HTTP
        checkUnsupportedConnector("<http:connector name=\"HttpConnector\" xmlns:http=\"http://www.mulesoft.org/schema/mule/http\"/>");
        //Polling HTTP
        checkUnsupportedConnector("<http:polling-connector name=\"PollingHttpConnector\"\n" +
                                  "        pollingFrequency=\"30000\" reuseAddress=\"true\" xmlns:http=\"http://www.mulesoft.org/schema/mule/http\"/>");
        //HTTPS
        checkUnsupportedConnector("<https:connector name=\"httpConnector\" xmlns:https=\"http://www.mulesoft.org/schema/mule/https\">\n" +
                                  "        <https:tls-key-store path=\"~/ce/tests/integration/src/test/resources/muletest.keystore\" keyPassword=\"mulepassword\" storePassword=\"mulepassword\"/>\n" +
                                  "</https:connector>");
        //JMS
        checkUnsupportedConnector("<jms:activemq-connector name=\"Active_MQ\" brokerURL=\"vm://localhost?broker.persistent=false&amp;broker.useJmx=false\" validateConnections=\"true\" xmlns:jms=\"http://www.mulesoft.org/schema/mule/jms\"/>");
        //XMPP
        //Doesn't work offline
        //checkUnsupportedConnector("<xmpp:connector name=\"xmppConnector\" user=\"muleinaction@jabber.org\" password=\"manning\" host=\"jabber.org\" xmlns:xmpp=\"http://www.mulesoft.org/schema/mule/xmpp\"/>");
        //FTP
        checkUnsupportedConnector("<ftp:connector name=\"ftpConnector\" streaming=\"true\" xmlns:ftp=\"http://www.mulesoft.org/schema/mule/ftp\"/>");
        //SFTP
        checkUnsupportedConnector("<sftp:connector name=\"sftp-default\" xmlns:sftp=\"http://www.mulesoft.org/schema/mule/sftp\"/>");
        //File Input
        checkUnsupportedConnector("<file:connector name=\"output\" outputAppend=\"false\" outputPattern=\"#[function:datestamp]-#[header:inbound:originalFilename]\" xmlns:file=\"http://www.mulesoft.org/schema/mule/file\"/>\n");
        //File Output
        checkUnsupportedConnector("<file:connector name=\"input\" fileAge=\"500\" autoDelete=\"true\" pollingFrequency=\"100\" moveToDirectory=\"/backup\" moveToPattern=\"#[header:inbound:originalFilename].backup\" xmlns:file=\"http://www.mulesoft.org/schema/mule/file\"/>\n");
        //VM
        checkUnsupportedConnector("<vm:connector name=\"memory\" xmlns:vm=\"http://www.mulesoft.org/schema/mule/vm\"/>");

    }

    private void checkUnsupportedConnector(String connectorConfig) throws IOException, SAXException, MuleArtifactFactoryException
    {
        Document document = XMLUnit.buildControlDocument(connectorConfig);

        MuleArtifact artifact = lookupArtifact().getArtifact(document.getDocumentElement(), getXmlConfigurationCallbackForUnsupportedConnector());

        Assert.assertNotNull(artifact);
        Assert.assertFalse(artifact.hasCapability(Testable.class));
        Assert.assertEquals(artifact.getCapability(Testable.class), null);
    }

    private XmlConfigurationCallback getXmlConfigurationCallbackForUnsupportedConnector()
    {
        return new XmlConfigurationCallback()
        {
            @Override
            public Element getGlobalElement(String s)
            {
                return null;
            }

            @Override
            public String getSchemaLocation(String s)
            {
                if (s != null){
                    int connectorNameStart = s.lastIndexOf("/") + 1;
                    s = s + "/current/mule-" + s.substring(connectorNameStart) + ".xsd";
                    return s;
                }
                else return null;
            }

            public Element[] getPropertyPlaceholders()
            {
                return new Element[0];
            }

            public Map<String, String> getEnvironmentProperties()
            {
                return null;
            }
        };

    }

    protected static XmlConfigurationMuleArtifactFactory lookupArtifact()
    {
        return ServiceLoader.load(XmlConfigurationMuleArtifactFactory.class).iterator().next();
    }
}
