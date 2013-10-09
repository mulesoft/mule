/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
