/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.api.annotation;

import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation defines a class that will export its functionality as a Mule module.
 * <p/>
 * There are a few restrictions as to which types as valid for this annotation:
 * - It cannot be an interface
 * - It must be public
 * - It cannot have a typed parameter (no generic)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Module
{

    /**
     * The name of the module.
     */
    String name();

    // XML specifics should go to a capability?

    ///**
    // * The version of the module. Defaults to 1.0.
    // */
    //String schemaVersion() default DEFAULT_VERSION;
    ///**
    // * Namespace of the module
    // */
    //String namespace() default "";
    //
    ///**
    // * Location URI for the schema
    // */
    //String schemaLocation() default "";

    String version();

    /**
     * Minimum Mule version required
     */
    String minMuleVersion() default MuleExtension.MIN_MULE_VERSION;

    // does friendly name makes sense? I think not
    ///**
    // * Provides a friendly name for the module.
    // */
    //String friendlyName() default "";

    /*
    TODO: does this make sense? How do me model several configuration other than the auto magical OAuth support?
    if only automatic version is available, then we shouldn't expose this
    */
    /**
     * Name of the configuration element
     */
    String configElementName() default MuleExtensionConfiguration.DEFAULT_NAME;

    /**
     * Short description about the annotated module.
     */
    String description() default "";

    //String DEFAULT_VERSION = "1.0";

    ///**
    // * Whether connectivity testing is enabled or not in this connector
    // */
    //ConnectivityTesting connectivityTesting() default ConnectivityTesting.OFF;
    //
    ///**
    // * Whether metadata is enable or not in this connector
    // */
    //
    //MetaDataSwitch metaData() default MetaDataSwitch.OFF;

}