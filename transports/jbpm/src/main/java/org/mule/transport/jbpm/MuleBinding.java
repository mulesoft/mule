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

public class MuleBinding extends JpdlBinding
{
    public MuleBinding()
    {
        super("mule");
    }

    public Object parseJpdl(Element element, Parse parse, JpdlParser parser)
    {
        MuleActivity muleActivity = new MuleActivity();

        // Note: The last method argument is the default value.
        muleActivity.setSynchronous(XmlUtil.attributeBoolean(element, "synchronous", false, parse, true));
        muleActivity.setEndpoint(XmlUtil.attribute(element, "endpoint", true, parse));
        muleActivity.setTransformers(XmlUtil.attribute(element, "transformers", false, parse, null));
        // TODO: Map properties
        muleActivity.setPayload(XmlUtil.attribute(element, "payload", false, parse, null));
        muleActivity.setPayloadSource(XmlUtil.attribute(element, "payloadSource", false, parse, null));
        muleActivity.setVariableName(XmlUtil.attribute(element, "var", false, parse, null));

        return muleActivity;
    }
}
