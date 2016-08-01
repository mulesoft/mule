import org.mule.runtime.core.api.MuleEventContext
import org.mule.runtime.core.api.lifecycle.Callable

public class GroovyDynamicScript implements Callable
{
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        return eventContext.getMessage().getPayload() + " Received2"
    }
}
