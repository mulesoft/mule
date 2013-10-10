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
import java.util.Map;

/**
 * <p>
 * Used on Components that have an outbound endpoint configured or a response being
 * sent back, and enables users to set outbound/response headers via {@link Map}
 * without needing to use the Mule API (other than using the annotation) on the
 * method argument). This parameter annotation passes in a reference to a {@link Map}
 * that can be used to populate outbound headers that will be set with the outgoing
 * message. For example, when sending an email message, you may want to set
 * properties such as "from" or "subject" as a sender header.
 * </p>
 * <p>
 * This annotation must only be defined on a parameter of type {@link Map}.
 * </p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Evaluator("outboundHeaders")
public @interface OutboundHeaders
{
    // no custom methods
}
