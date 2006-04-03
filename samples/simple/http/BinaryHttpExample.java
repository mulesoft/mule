package http;

import org.mule.extras.client.MuleClient;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageHelper;
import util.SimpleRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestSuite;
import junit.framework.TestResult;
import file.TextFileExample;

public class BinaryHttpExample extends SimpleRunner
 {
    public BinaryHttpExample() {
        super("http/BinaryHTTPExample.xml");
    }

    public static void main(String[] args) {
        TestSuite suite = new TestSuite(BinaryHttpExample.class);
        TestResult result = new TestResult();
        suite.run(result);
    }

    protected void runSample() throws Exception {

        // create client & talk
        MuleClient client = new MuleClient();

        Object serviceArgs = Arrays.asList(new Integer[]{new Integer(42)});
        // talk to an abstract service name (no hardcoded physical address!)
        UMOMessage replyMsg = client.send("ServiceEndpoint", serviceArgs, null);

        assertNotNull(replyMsg);
        log("replyMsg: " + replyMsg);

        Object payload = replyMsg.getPayload();
        assertNotNull(payload);
        log("payload.class: " + payload.getClass());

        // manual conversion from byte[] to Object,
        // feature did not exist when I wrote this example
        Object obj = new ByteArrayToSerializable().doTransform(payload, null);
        log("object.class: " + obj.getClass());
        log("Received Response: " + obj);

        assertTrue(obj instanceof ArrayList);
        List results = (List)obj;
        assertEquals(2, results.size());
        assertEquals("Hello", results.get(0));
        assertEquals(":-)", results.get(1));
    }
}
