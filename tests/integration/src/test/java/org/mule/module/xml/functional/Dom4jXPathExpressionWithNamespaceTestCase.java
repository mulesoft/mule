/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.text.MessageFormat;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class Dom4jXPathExpressionWithNamespaceTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public static final String MESSAGE = "<foo:endpoint xmlns:foo=\"http://foo.com\">{0}</foo:endpoint>";

    public Dom4jXPathExpressionWithNamespaceTestCase(ConfigVariant variant, String configResources)
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
        return DocumentHelper.parseText(MessageFormat.format(MESSAGE, name));

    }

}
