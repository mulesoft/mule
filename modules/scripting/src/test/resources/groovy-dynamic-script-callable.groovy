import org.mule.api.MuleEventContext
import org.mule.api.lifecycle.Callable

public class GroovyDynamicScript implements Callable
{
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        return eventContext.getMessage().getPayloadAsString() + " Received2"
    }
}