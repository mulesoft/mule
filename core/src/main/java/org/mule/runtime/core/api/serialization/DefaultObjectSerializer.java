/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.serialization;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import org.mule.api.config.MuleConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A {@link Qualifier} annotation for injecting
 * the {@link ObjectSerializer} which has been
 * selected as the default in the {@link MuleConfiguration}
 *
 * @since 3.7.0
 */
@Qualifier
@Retention(RUNTIME)
@Target({TYPE, ElementType.METHOD, FIELD, PARAMETER})
public @interface DefaultObjectSerializer
{

}
