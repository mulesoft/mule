/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
