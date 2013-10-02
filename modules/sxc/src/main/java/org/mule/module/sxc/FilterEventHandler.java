/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.sxc;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import com.envoisolutions.sxc.xpath.XPathEvent;
import com.envoisolutions.sxc.xpath.XPathEventHandler;

import javax.xml.stream.XMLStreamException;

public class FilterEventHandler extends XPathEventHandler
{
    SxcFilteringOutboundRouter router;
    SxcFilter filter;
    
    public FilterEventHandler(SxcFilteringOutboundRouter router, SxcFilter filter)
    {
        super();
        this.router = router;
        this.filter = filter;
    }
    
    @Override
    public void onMatch(XPathEvent event) throws XMLStreamException
    {
        try 
        {
            MuleMessage msg = SxcFilteringOutboundRouter.getCurrentMessage();
            msg.setInvocationProperty(filter.toString(), true);
            
            if (router.testMatch(msg))
            {
                throw new StopProcessingException();
            }
        }
        catch (UndefinedMatchException e) 
        {
            // ignore
        }
        catch (MuleException e)
        {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

}


