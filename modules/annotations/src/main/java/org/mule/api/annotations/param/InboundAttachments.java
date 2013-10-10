/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.annotations.meta.Evaluator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on component and transformer methods that have a {@link org.mule.api.annotations.Transformer} annotation.
 * This annotation marks the method parameter that will be used to pass in one or more of the received attachments.
 * This annotation can define a single attachment, a comma-separated list of attachment names, or '*' to denote all headers. By default,
 * if a named header is not present, an exception will be thrown. However, if the header name is defined with the '?' post fix, it
 * will be marked as optional.
 * <p/>
 * When defining multiple attachment names i.e. InboundAttachments("shipping-slip.pdf, customer-record.xml") or using the '*' wildcard to denote all attachments,
 * or wildcard expressions can be used, such as '*.pdf' or multiple patterns such as '*.pdf, *.xml'.
 * the parameter can be a {@link java.util.Map} or {@link java.util.List}. If a {@link java.util.Map} is used, the header name and value is passed in.
 * If {@link java.util.List} is used, just the header value is used. If a single header name is defined, the header type can be used as the parameter or
 * {@link java.util.List} or {@link java.util.Map} can be used too.  Entry type for collections is {@link javax.activation.DataHandler}.
 *
 * The Inbound attachments collection is immutable, so the attachments Map or List passed in will be immutable too. Attempting to write to the Map or List will result in an {@link UnsupportedOperationException}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("attachments")
//
public @interface InboundAttachments
{
    /**
     * Defines the headers that should be injected into the parameter. This can be a single header, a comma-separated
     * list of header names, or '*' to denote all headers or a comma separated list of wildcard expressions such as '*.pdf, *.xml'.
     * By default, if a named header is not present, an exception will
     * be thrown. However, if the header name is defined with the '?' post fix, it will be marked as optional. When using wildcard expressions
     * the optional '?' postfix cannot be used.
     *
     * @return the attachment expression used to query the message
     */
    String value();
}
