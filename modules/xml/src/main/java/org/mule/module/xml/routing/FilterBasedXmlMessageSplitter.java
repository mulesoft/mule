/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.routing;

import org.mule.api.lifecycle.InitialisationException;

/**
 * This splitter will select the endpoint to send a message part on by filtering parts using the endpoint filters.
 */
public class FilterBasedXmlMessageSplitter extends XmlMessageSplitter
{
    public FilterBasedXmlMessageSplitter()
    {
        //By disabling this, the endpoints will be invoked with the first endpoint being checked first
        //and its filter applied before it is used
        this.setDisableRoundRobin(true);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        logger.warn("Deprecation warning: The FilteringXmlMessageSplitter router has been deprecating in Mule 2.2 in favour of using the <expression-splitter> router.");
        super.initialise();
    }
}
