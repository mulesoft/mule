package org.mule.providers.cxf.jaxws;

import org.mule.umo.UMOEventContext;

public class ClientMessageGenerator implements org.mule.umo.lifecycle.Callable 
{
    
    public Object onCall(UMOEventContext eventContext) throws Exception 
    {
        return generate();
    }

    public String generate() 
    {
        return "Dan";
    }
}
