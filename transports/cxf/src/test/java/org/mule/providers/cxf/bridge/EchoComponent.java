
package org.mule.providers.cxf.bridge;

import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;

import javax.xml.transform.Source;

public class EchoComponent implements Callable
{

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        UMOMessage message = eventContext.getMessage();
        Source s = (Source) message.getPayload();
        return s;
    }

}
