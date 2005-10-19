package org.mule.extras.oracle.jms;

import javax.jms.Message;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class OracleJmsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    public UMOMessageAdapter createAdapter(Object payload) throws Exception
    {
        return new OracleJmsMessageAdapter((Message) payload);
    }

    public Object getValidMessage() throws Exception
    {
        return OracleJmsConnectorTestCase.getMessage();
    }
}
