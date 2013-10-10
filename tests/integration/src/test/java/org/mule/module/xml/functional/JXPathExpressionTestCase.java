/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.text.MessageFormat;
import java.util.Properties;

public class JXPathExpressionTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public static final String MESSAGE = "<endpoint>{0}</endpoint>";

    public JXPathExpressionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/endpoint");
        p.setProperty("selector.evaluator", "jxpath");

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

    protected String documentFor(String name) throws Exception
    {
        return MessageFormat.format(MESSAGE, name);
    }

}
