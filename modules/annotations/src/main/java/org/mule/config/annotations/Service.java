/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations;

import org.mule.impl.annotations.ServiceScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that defines a Mule service. Objects registered with this annotation will
 * be configured as a service in Mule
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service
{
    /**
     * The name of this service
     * @return the name of this service. This value is required
     */
    String name();

    /**
     * Determines if this service will be a singleton or prototype.
     * Note this refers to the actual service object instance i.e. your annotated object.
     *
     * By default singleton is used so that any fields in your objects will retain thier values, making stateful
     * services possible by default. Note that any operations on field variables will need to be thread-safe.
     * @return true if the service is a singleton
     */
    ServiceScope scope() default ServiceScope.SINGLETON;

    /**
     * The service builder is used to add configuration to this service.  A builder is used when the service has more
     * complicated requirements above what the annotations offer. Usually this means more complex routing behavior.
     *
     * This builder property can be a reference to an implmentation of {@link ServiceBuilder} interface that can be in
     * a local registry or Galaxy.
     * TODO add details of how this is configured
     *
     * If the service implements the {@link ServiceBuilderAware} interface, this property is ignored.
     * @return the reference name or location of the {@link ServiceBuilder} implementation to use to construct this
     * service or an empty String if there is no {@link ServiceBuilder} for this service.
     */
    //TODO Implement
    String builder() default "";
}
