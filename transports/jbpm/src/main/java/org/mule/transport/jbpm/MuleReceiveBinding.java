/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
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
        MuleReceiveActivity activity = new MuleReceiveActivity();

        // Note: The last method argument is the default value.
        activity.setVariableName(XmlUtil.attribute(element, "var", false, parse, null));
        activity.setEndpoint(XmlUtil.attribute(element, "endpoint", false, parse, null));
        activity.setPayloadClass(XmlUtil.attribute(element, "type", false, parse, null));

        return activity;
    }
}
