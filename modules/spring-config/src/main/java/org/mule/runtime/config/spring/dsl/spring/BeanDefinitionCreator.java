/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

/**
 * Abstract construct of a chain of responsibility to create a {@link org.springframework.beans.factory.config.BeanDefinition}
 * from a {@code org.mule.runtime.config.spring.dsl.model.ComponentModel}.
 *
 * @since 4.0
 */
public abstract class BeanDefinitionCreator
{

    private BeanDefinitionCreator successor;

    /**
     * @param nextBeanDefinitionCreator next processor in the chain.
     */
    public void setSuccessor(BeanDefinitionCreator nextBeanDefinitionCreator)
    {
        this.successor = nextBeanDefinitionCreator;
    }

    /**
     * Will iterate over the chain of processors until there's one that handle
     * the request by return true to {@code #handleRequest}.
     *
     * @param request
     */
    public final void processRequest(CreateBeanDefinitionRequest request)
    {
        if (handleRequest(request))
        {
            return;
        }
        if (successor != null)
        {
            successor.processRequest(request);
        }
    }

    /**
     * Instances of {@code BeanDefinitionCreator} that will be responsible
     * to create the {@code BeanDefinition} must return true to this call,
     * otherwise they must do nothing.
     *
     * @param createBeanDefinitionRequest the creation request.
     * @return true if it created the {@code BeanDefinition}, false otherwise.
     */
    abstract boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest);

}
