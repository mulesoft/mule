/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
