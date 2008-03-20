
package org.mule.test.integration.streaming;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

public class SimpleExceptionThrowingTestComponent implements Callable
{

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        // if (eventContext.getMessage().getPayload() instanceof InputStream)
        // {
        // ((InputStream) eventContext.getMessage().getPayload()).close();
        //        }
        throw new RuntimeException("Runtim Exception");
    }

}
