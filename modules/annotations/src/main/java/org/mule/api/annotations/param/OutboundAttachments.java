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
 * <p>
 * Used on component methods, this parameter annotation passes in a reference to a
 * {@link java.util.Map} that can be used to populate outbound attachments that will
 * be set with the outgoing message. For example, when sending an email message, you
 * may want to add attachments such as images or documents.
 * </p>
 * <p>
 * This annotation must only be defined on a parameter of type {@link java.util.Map}.
 * The elements in the map will be of type {@link javax.activation.DataHandler}, thus
 * the annotated parameter should be defined as
 * <code>java.util.Map&lt;java.lang.String, javax.activation.DataHandler&gt;</code>
 * where the key is the attachment name and the value is the handler for the
 * attachment.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("outboundAttachments")
public @interface OutboundAttachments
{
    // no custom methods
}
