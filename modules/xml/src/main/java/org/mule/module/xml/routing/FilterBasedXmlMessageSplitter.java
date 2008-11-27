/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        super.initialise();;
    }
}
