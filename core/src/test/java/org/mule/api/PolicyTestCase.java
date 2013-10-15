/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.policy.AroundPolicy;
import org.mule.api.processor.policy.PolicyInvocation;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class PolicyTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testSinglePolicy() throws Exception
    {
        AroundPolicy ap = new TestPolicy("test around policy");

        // this is our regular chain that should get a policy applied
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(
                                                             new StringAppendTransformer("first"),
                                                             new StringAppendTransformer(" second"));
        initialiseObject(chain);

        // test registration
        assertEquals("No policies should have been registered.", 0, chain.getPolicies().list().size());
        chain.getPolicies().add(ap);
        assertSame("Policy has not been registered.", ap, chain.getPolicies().list().iterator().next());

        System.out.println(chain);

        // invoke
        final MuleEvent result = chain.process(getTestEvent("payload "));
        assertNotNull(result);
        final MuleMessage message = result.getMessage();
        assertNotNull(message);
        assertEquals("payload {before} first second {after}", message.getPayload());

        // test cleanup
        final AroundPolicy policy = chain.getPolicies().remove(ap.getName());
        assertSame("Wrong policy returned?", ap, policy);
        assertEquals("No policies should have been registered.", 0, chain.getPolicies().list().size());
    }

    @Test
    public void testMultiplePolicies() throws Exception
    {

        // this is our regular chain that should get a policy applied
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(
                                                            new StringAppendTransformer("first"),
                                                            new StringAppendTransformer(" second"));
        initialiseObject(chain);

        // test registration
        assertEquals("No policies should have been registered.", 0, chain.getPolicies().list().size());
        AroundPolicy policy1 = new TestPolicy("test around policy 1");
        chain.getPolicies().add(policy1);
        // add another policy
        final TestPolicy policy2 = new TestPolicy("test around policy 2");
        chain.getPolicies().add(policy2);
        assertEquals("Wrong policies count.", 2, chain.getPolicies().list().size());

        System.out.println(chain);

        // invoke
        final MuleEvent result = chain.process(getTestEvent("payload "));
        assertNotNull(result);
        final MuleMessage message = result.getMessage();
        assertNotNull(message);
        assertEquals("payload {before} {before} first second {after} {after}", message.getPayload());

        // test cleanup
        final AroundPolicy policy = chain.getPolicies().remove(policy1.getName());
        assertSame("Wrong policy returned?", policy1, policy);
        chain.getPolicies().remove(policy2.getName());
        assertEquals("No policies should have been registered.", 0, chain.getPolicies().list().size());
    }

    @Test
    public void testDuplicateName() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from();
        chain.getPolicies().add(new TestPolicy("test"));
        try
        {
            chain.getPolicies().add(new TestPolicy("test"));
            fail("Should've thrown an exception, no duplicates allowed");
        }
        catch (IllegalArgumentException e)
        {
            System.out.println(e);
            // expected
        }
    }

    private static class TestPolicy implements AroundPolicy
    {

        private String name;

        public TestPolicy(final String name)
        {
            this.name = name;
        }

        public MuleEvent invoke(PolicyInvocation invocation) throws MuleException
        {
            // mutates the event directly, thus we can safely ignore the return object
            new StringAppendTransformer("{before} ").process(invocation.getEvent());
            final MuleEvent result = invocation.proceed();
            //throw new DefaultMuleException("test");
            new StringAppendTransformer(" {after}").process(invocation.getEvent());
            return result;
        }

        public String getName()
        {
            return name;
        }
    }
}
