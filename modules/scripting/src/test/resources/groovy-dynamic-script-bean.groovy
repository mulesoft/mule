import org.mule.api.MuleEventContext
import org.mule.api.lifecycle.Callable

public class GroovyDynamicScriptBean implements Callable
{
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        return eventContext.getMessage().getPayloadAsString() + " Received2"
    }
}
