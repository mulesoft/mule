/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


