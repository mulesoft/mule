/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
