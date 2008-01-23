package org.mule.transport.cxf.jaxws;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;


public class ClientMessageGenerator implements Callable 
{
    
    public Object onCall(MuleEventContext eventContext) throws Exception 
    {
        return generate();
    }

    public String generate() 
    {
        return "Dan";
    }
}
