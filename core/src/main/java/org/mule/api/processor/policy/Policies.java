/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor.policy;

import org.mule.api.processor.MessageProcessorChain;
import org.mule.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;

public class Policies
{

    private final MessageProcessorChain messageProcessorChain;
    private LinkedList<AroundPolicy> policies = new LinkedList<AroundPolicy>();

    public Policies(MessageProcessorChain messageProcessorChain)
    {
        this.messageProcessorChain = messageProcessorChain;
    }

    public void add(AroundPolicy policy)
    {
        // TODO concurrency
        if (findPolicy(policy.getName()) != null)
        {
            final String msg = String.format("There's already a policy registered under name [%s] for chain [%s]:%s",
                                             policy.getName(), messageProcessorChain.getName(), messageProcessorChain);
            throw new IllegalArgumentException(msg);
        }
        this.policies.add(policy);
    }

    public AroundPolicy removePolicy(String policyName)
    {
        // TODO concurrency
        final AroundPolicy policy = findPolicy(policyName);
        if (policy == null)
        {
            return null;
        }
        this.policies.remove(policy);

        return policy;
    }

    public List<AroundPolicy> listActive()
    {
        // TODO concurrency
        return Collections.unmodifiableList(this.policies);
    }

    public void clearPolicies()
    {
        // TODO concurrency
        this.policies.clear();
    }

    /**
     * @return policy with that name or null if not found
     */
    protected AroundPolicy findPolicy(String policyName)
    {
        // find { policy.name == policyName }
        return (AroundPolicy) CollectionUtils.find(this.policies,
                                                   new BeanPropertyValueEqualsPredicate("name", policyName));
    }
}
