/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.expression;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An expression annotation that allows developers to control how method parameter values are injected on a service component or transformer.
 * This parameter level annotation enables an XPath expression to be execute on an Xml payload of a message.
 *
 * The annotation uses the standard JAXP api and this dictates what method parameter types the annotaiton can be used with. The follonig parameter types are supported
 * <ul>
 * <li>{@link Boolean}</li>
 * <li>{@link String}</li>
 * <li>{@link Double}</li>
 * <li>{@link org.w3c.dom.Node}</li>
 * <li>{@link org.w3c.dom.NodeList}</li>
 * <li>{@link org.w3c.dom.Element}</li>
 * <li>{@link org.w3c.dom.Document}</li>
 * </ul>
 *
 * See {@link javax.xml.xpath.XPathConstants} for further information.
 * 
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("xpath2")
public @interface XPath
{
    String value();

    boolean optional() default false;
}
