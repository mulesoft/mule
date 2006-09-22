/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.UMOTransformer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmlUMOMessageTransformersTestCase extends AbstractXmlTransformerTestCase
{
    private UMOMessage testObject = null;

    protected void doSetUp() throws Exception {
        RequestContext.setEvent(new MuleEvent(testObject, getTestEndpoint("test", "sender"), MuleTestUtils.getTestSession(), true));
    }

    protected void doTearDown() throws Exception {
        RequestContext.clear();
    }

    public XmlUMOMessageTransformersTestCase() {
        Map props = new HashMap();
        props.put("object", new Apple());
        props.put("number", new Integer(1));
        props.put("string", "hello");
        testObject = new MuleMessage("test", props);
    }

    public UMOTransformer getTransformer() throws Exception
    {
        ObjectToXml trans = new ObjectToXml();
        trans.setSourceType(UMOMessage.class.getName());
        return trans;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new XmlToObject();
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return "<org.mule.impl.MuleMessage>\n" +
                "  <adapter class=\"org.mule.providers.DefaultMessageAdapter\">\n" +
                "    <message class=\"string\">test</message>\n" +
                "    <id>" + testObject.getUniqueId() + "</id>\n" +
                "    <properties class=\"edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap\">\n" +
                "      <entry>\n" +
                "        <string>object</string>\n" +
                "        <org.mule.tck.testmodels.fruit.Apple>\n" +
                "          <bitten>false</bitten>\n" +
                "          <washed>false</washed>\n" +
                "        </org.mule.tck.testmodels.fruit.Apple>\n" +
                "      </entry>\n" +
                "      <entry>\n" +
                "        <string>string</string>\n" +
                "        <string>hello</string>\n" +
                "      </entry>\n" +
                "      <entry>\n" +
                "        <string>number</string>\n" +
                "        <int>1</int>\n" +
                "      </entry>\n" +
                "    </properties>\n" +
                "    <attachments  class=\"edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap\"/>\n" +
                "    <encoding>UTF-8</encoding>\n" +
                "  </adapter>\n" +
                "</org.mule.impl.MuleMessage>";
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }
        if (src == null || result == null) {
            return false;
        }
        if(src instanceof UMOMessage && result instanceof UMOMessage) {
            return ((UMOMessage)src).getPayload().equals(((UMOMessage)result).getPayload()) &&
                 ((UMOMessage)src).getProperty("object").equals(((UMOMessage)result).getProperty("object")) &&
                 ((UMOMessage)src).getProperty("string").equals(((UMOMessage)result).getProperty("string")) &&
                 ((UMOMessage)src).getIntProperty("number", -1) == ((UMOMessage)result).getIntProperty("number", -2);
        } else {
            return false;
        }
    }

}
