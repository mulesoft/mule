/*
 * $Id:BinaryHttpExample.java 2854 2006-08-29 22:49:34 +0000 (Tue, 29 Aug 2006) tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.mule.extras.client.MuleClient;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.umo.UMOMessage;

public class BinaryHttpExample extends SimpleRunner
 {
    public BinaryHttpExample() {
        super("BinaryHttpExample.xml");
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
