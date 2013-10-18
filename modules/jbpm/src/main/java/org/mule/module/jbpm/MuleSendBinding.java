/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

public class MuleSendBinding extends JpdlBinding
{
    public MuleSendBinding()
    {
        super("mule-send");
    }

    @Override
    public Object parseJpdl(Element element, Parse parse, JpdlParser parser)
    {
        MuleSendActivity activity = new MuleSendActivity();

        activity.setEndpoint(XmlUtil.attribute(element, "endpoint", parse));

        if (element.hasAttribute("exchange-pattern"))
        {
            activity.setMessageExchangePattern((XmlUtil.attribute(element, "exchange-pattern", parse)));
        }
        
        if (element.hasAttribute("expr"))
        {
            activity.setPayloadExpression(XmlUtil.attribute(element, "expr", parse));
        }
        
        if (element.hasAttribute("var"))
        {
            activity.setResponseVariableName(XmlUtil.attribute(element, "var", parse));
        }
        
        if (element.hasAttribute("type"))
        {
            activity.setResponsePayloadClass(XmlUtil.attribute(element, "type", parse));
        }

        return activity;
    }
}
