/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.api.tls.TlsContextFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A delegate object used to parse infrastructure elements.
 * <p>
 * By infrastructure, we mean things that are not particular to
 * an extension but a core Mule component such as a {@link ThreadingProfile},
 * {@link RetryPolicyTemplate}, {@link TlsContextFactory}, etc.
 *
 * @since 4.0
 */
interface InfrastructureParserDelegate
{

    /**
     * Determines if {@code this} instance is capable of handling
     * the given {@code element}
     *
     * @param element an XML element to be parsed
     * @return whether the given {@code element} is parseable with {@code this} instance
     */
    boolean accepts(Element element);

    /**
     * Parses the {@code element} by adding an entry into the {@code managedMap}, which
     * has the objects {@link Class} as keys and a {@link BeanDefinition} as values. Notice
     * that since the infrastructure object's {@link Class} acts as the map's key, only one
     * instance of the same class is expected by parsing round.
     *
     * @param element       an XML element to be parsed
     * @param managedMap    a {@link ManagedMap} on which the parsed {@link BeanDefinition} is stored
     * @param parserContext the current {@link ParserContext}
     */
    void parse(Element element, ManagedMap<Class<?>, BeanDefinition> managedMap, ParserContext parserContext);
}
