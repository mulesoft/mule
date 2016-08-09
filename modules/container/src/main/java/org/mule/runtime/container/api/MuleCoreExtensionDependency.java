/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated method must be used to inject a {@link MuleCoreExtension} dependency.
 * <p>
 * Example: how to define a dependency no DummyCoreExtension extension
 * </p>
 * 
 * <pre>
 *    <code>{@literal @}MuleCoreExtensionDependency
 *     public void setCoreExtension(DummyCoreExtension extension)
 *     {
 *         this.extension = extension;
 *     }
 *    </code>
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MuleCoreExtensionDependency {

}
