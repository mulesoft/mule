/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.model.JpdlProcessDefinition;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

public class MuleReceiveBinding extends JpdlBinding
{
    public MuleReceiveBinding()
    {
        super("mule-receive");
    }

    public Object parseJpdl(Element element, Parse parse, JpdlParser parser)
    {
        MuleReceiveActivity activity;
        
        JpdlProcessDefinition processDefinition = parse.contextStackFind(JpdlProcessDefinition.class);        
        if (processDefinition.getInitial() == null) 
        {
            processDefinition.setInitial(parse.contextStackFind(ActivityImpl.class));          
            activity = new MuleReceiveActivity(true);
        } 
        else
        {
            activity = new MuleReceiveActivity(false);
        }

        if (element.hasAttribute("var"))
            activity.setVariableName(XmlUtil.attribute(element, "var", parse));
        
        if (element.hasAttribute("endpoint"))
            activity.setEndpoint(XmlUtil.attribute(element, "endpoint", parse));
        
        if (element.hasAttribute("type"))
            activity.setPayloadClass(XmlUtil.attribute(element, "type", parse));

        return activity;
    }
}
