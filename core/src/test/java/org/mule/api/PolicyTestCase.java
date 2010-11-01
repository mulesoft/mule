/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.processor.policy.AroundPolicy;
import org.mule.api.processor.policy.PolicyInvocation;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

/**
 *
 */
public class PolicyTestCase extends AbstractMuleTestCase
{

    public void testPolicy() throws Exception
    {
        AroundPolicy ap = new AroundPolicy()
        {
            public MuleEvent invoke(PolicyInvocation invocation) throws MuleException
            {
                // mutates the event directly, thus we can safely ignore the return object
                new StringAppendTransformer(" {before} ").process(invocation.getEvent());
                final MuleEvent result = invocation.proceed();
                //throw new DefaultMuleException("test");
                new StringAppendTransformer(" {after}").process(invocation.getEvent());
                return result;
            }

            public String getName()
            {
                return "test around policy";
            }
        };

        // this is our regular chain that should get a policy applied
        DefaultMessageProcessorChain chain = new DefaultMessageProcessorChain(
                                                             new StringAppendTransformer("first"),
                                                             new StringAppendTransformer(" second"));
        initialiseObject(chain);

        // test registration
        assertEquals("No policies should have been registered.", 0, chain.getActivePolicies().size());
        chain.add(ap);
        assertSame("Policy has not been registered.", ap, chain.getActivePolicies().iterator().next());

        System.out.println(chain);

        // invoke
        final MuleEvent result = chain.process(getTestEvent("main"));
        assertNotNull(result);
        final MuleMessage message = result.getMessage();
        assertNotNull(message);
        assertEquals("main {before} first second {after}", message.getPayload());

        // test cleanup
        final AroundPolicy policy = chain.removePolicy(ap.getName());
        assertSame("Wrong policy returned?", ap, policy);
        assertEquals("No policies should have been registered.", 0, chain.getActivePolicies().size());
    }
}
