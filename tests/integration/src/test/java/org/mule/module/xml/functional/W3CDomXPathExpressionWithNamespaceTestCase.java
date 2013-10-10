/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class W3CDomXPathExpressionWithNamespaceTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public static final String MESSAGE = "<foo:endpoint xmlns:foo=\"http://foo.com\">{0}</foo:endpoint>";

    public W3CDomXPathExpressionWithNamespaceTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/foo:endpoint");
        p.setProperty("selector.evaluator", "xpath");

        return p;
    }

    @Override
    protected Object getMatchMessage() throws Exception
    {
        return documentFor("matchingEndpoint1");
    }

    @Override
    protected Object getErrorMessage() throws Exception
    {
        return documentFor("missingEndpoint");
    }

    protected Document documentFor(String name) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        String s = MessageFormat.format(MESSAGE, name);
        Document doc = builder.parse(new ByteArrayInputStream(s.getBytes()));
        return doc;
    }

}
