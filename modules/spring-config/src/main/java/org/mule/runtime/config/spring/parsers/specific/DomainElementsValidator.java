/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.ElementValidator;
import org.mule.config.spring.parsers.PreProcessor;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A {@link PreProcessor} that validates that no mule element
 * is used inside a &lt;spring:beans&gt; element.
 *
 * This is for security and consistency reasons. A way of enforcing
 * that XSD is not cheated
 *
 * @since 3.6.0
 */
public class DomainElementsValidator implements ElementValidator
{

    @Override
    public void validate(Element element)
    {
        Node node = element.getParentNode();

        while (node != null)
        {
            Node parent = node.getParentNode();
            if (parent == null)
            {
                return;
            }

            if (node.getNamespaceURI().contains("mule") && "beans".equals(parent.getLocalName()) && parent.getNamespaceURI().contains("spring"))
            {
                throw new IllegalStateException("Mule elements are not allowed inside Spring elements when used in a domain");
            }

            node = parent;
        }
    }

}
