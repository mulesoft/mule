/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;


import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * A specialization of {@link DefaultListableBeanFactory} mainly to
 * work around <a href="https://jira.spring.io/browse/SPR-12914">
 * Spring issue SPR-12914</a>.
 * <p/>
 * For now, this class whole purpose is to intercept the return value
 * of the {@link #findAutowireCandidates(String, Class, DependencyDescriptor)}
 * method and filter it by using a {@link Jsr330AutowireCandidateFilter}.
 * <p/>
 * When the Spring issue is fixed, the need for this class should be reconsidered.
 *
 * @see Jsr330AutowireCandidateFilter
 * @since 3.7.0
 */
final class MuleBeanFactory extends DefaultListableBeanFactory
{

    private final Jsr330AutowireCandidateFilter autowireCandidateFilter = new Jsr330AutowireCandidateFilter();

    MuleBeanFactory(BeanFactory parentBeanFactory)
    {
        super(parentBeanFactory);
    }

    @Override
    protected Map<String, Object> findAutowireCandidates(String beanName, Class<?> requiredType, DependencyDescriptor descriptor)
    {
        Map<String, Object> candidates = super.findAutowireCandidates(beanName, requiredType, descriptor);
        return autowireCandidateFilter.filter(candidates, descriptor);
    }
}
