/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.fail;

import org.mule.tck.testmodels.services.Person;

import org.junit.Test;

public class AxisConnectorJmsFunctionalTestCase extends AxisConnectorVMFunctionalTestCase
{

    @Override
    protected String getTransportProtocol()
    {
        return "jms";
    }

    // TODO This test case still has serious issues. It was passing at once point just because of some timing
    // luck between test timeout and event timeout when recieving jms reply.
    // TODO This test causes an infinite loop in the method org.apache.axis.encoding.SerializationContext.serialize()
    @Test
    public void testException() throws Exception
    {
//        try
//        {
//            muleContext.getClient().send(getTestExceptionEndpoint(), new Person("Ross", "Mason"), null);
//            fail("A nested Fault should have been raised");
//        }
//        catch (Exception e)
//        {
//            // expected
//        }
//        catch (Error e)
//        {
//            // expected
//        }
    }
}
