/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.soap.xfire;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.handler.AbstractHandler;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mule.config.MuleProperties;
import org.mule.providers.soap.MuleSoapHeaders;

/**
 * Reads the Mule Soap Header and sets the various header properties on the context
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleHeadersInHandler extends AbstractHandler
 {
    protected Namespace ns = Namespace.getNamespace(MuleSoapHeaders.MULE_NAMESPACE, MuleSoapHeaders.MULE_10_ACTOR);

    /**
     * Invoke a handler. If a fault occurs it will be handled via the
     * <code>handleFault</code> method.
     *
     * @param context The message context.
     */
    public void invoke(MessageContext context) throws Exception
    {
        if(context.getInMessage()!=null) {
            Element header = context.getInMessage().getHeader();
            if(header==null) return;

            Element muleHeaders = header.getChild(MuleSoapHeaders.MULE_HEADER, ns);
            if(muleHeaders!=null) {
                Element child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_ID_PROPERTY, ns);
                if(child!=null) {
                    context.setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, child.getText());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, ns);
                if(child!=null) {
                    context.setProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, child.getText());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, ns);
                if(child!=null) {
                    context.setProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, child.getText());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_REPLY_TO_PROPERTY, ns);
                if(child!=null) {
                    context.setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, child.getText());
                }
            }
        }
    }

}
