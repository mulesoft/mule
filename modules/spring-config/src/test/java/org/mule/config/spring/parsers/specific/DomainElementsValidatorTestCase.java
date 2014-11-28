/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SmallTest
public class DomainElementsValidatorTestCase extends AbstractMuleTestCase
{

    public static final String MULE_NAMESPACE = "http://mule.org/mule.xsd";
    private DomainElementsValidator preProcessor = new DomainElementsValidator();

    @Test(expected = IllegalStateException.class)
    public void rejects()
    {
        Node parent = mock(Node.class);
        when(parent.getNamespaceURI()).thenReturn("http://springframework.org/beans.xsd");
        when(parent.getLocalName()).thenReturn("beans");
        preProcessor.validate(buildMuleElement(parent));
    }

    @Test
    public void accepts()
    {
        preProcessor.validate(buildMuleElement(mock(Node.class)));
    }

    private Element buildMuleElement(Node grandParent)
    {
        Element element = mock(Element.class, RETURNS_DEEP_STUBS);
        when(element.getNamespaceURI()).thenReturn(MULE_NAMESPACE);

        Node parent = mock(Node.class);
        when(parent.getNamespaceURI()).thenReturn(MULE_NAMESPACE);

        when(parent.getParentNode()).thenReturn(grandParent);
        when(element.getParentNode()).thenReturn(parent);

        return element;
    }
}
