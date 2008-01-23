
package org.mule.transport.cxf.bridge;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import javax.xml.transform.Source;

public class EchoComponent implements Callable
{

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        MuleMessage message = eventContext.getMessage();
        Source s = (Source) message.getPayload();
        return s;
    }

}
