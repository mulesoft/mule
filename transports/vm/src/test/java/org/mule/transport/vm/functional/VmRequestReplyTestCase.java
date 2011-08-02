package org.mule.transport.vm.functional;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import static org.junit.Assert.assertThat;

public class VmRequestReplyTestCase extends FunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "vm/vm-request-reply-config.xml";
    }

    @Test
    public void testVmReplyTo() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://in4vm", "some data", null);
        assertThat(result, IsNull.<Object>notNullValue());
        assertThat(result.getExceptionPayload(), IsNull.<Object>nullValue());
        assertThat(result.getPayload() instanceof NullPayload, Is.is(false));
        assertThat(result.getPayloadAsString(), Is.is("HELLO"));
    }
}
