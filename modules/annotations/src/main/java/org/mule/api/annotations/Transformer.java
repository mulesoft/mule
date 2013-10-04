/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations;

import org.mule.transformer.types.MimeTypes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a that a class contains methods which are Mule Transformer, which means it will be made available in the Mule container.
 * Transformers are used to convert one object type to another.  Mule uses them to provide automatic conversion of Java types
 * and now support Mime type conversion too.
 *
 * The parameters passed into the method define the source object(s) to transform, the return type of the method defines the return object type.
 * Transformers can define additional source types, that when received will be automatically converted to the parameter type accepted by
 * the annotated method.
 *
 * There are some rules to follow when writing a transformer method -
 * <ol>
 * <li>The method's declaring class must be annotated with ContainsTransformerMethods</li>
 * <li>The annotation must appear on a concrete method, not on an abstract or interface method</li>
 * <li>The method must be public</li>
 * <li>The method must have a non-void return type</li>
 * <li>The method must have at least one parameter argument</li>
 * </ol>
 *
 * It is good practice to define any custom transformers in their own class (a class can have more than one transformer method).
 * A transformer class should be thread-safe and not have any transitive state, meaning that it should not maintain state as
 * a result of a transformation. It is fine for transformers to have configuration state, such as in an XSLT or XQuery template file
 * (note that Mule already provides transformers for XSLT and XQuery).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transformer
{
    /**
     * The 'priorityWeighting property is used to resolve conflicts where there is more than one transformers that match
     * the selection criteria.  10 is the highest priority and 1 is the lowest.
     *
     * @return the priority weighting for this transformer. If the class defines more than one transform method, every transform
     *         method will have the same weighting.
     */
    int priorityWeighting() default 5;

    /**
     * Source mime type describes the acceptable MIME type of this transformer input.
     *
     * @return The supported MIME type for input.
     * @since 3.3.0
     */
    String sourceMimeType() default MimeTypes.ANY;

    /**
     * The result MIME type describes the MIME type of this transformer output.
     *
     * @return The MIME type for the output of this transformer.
     * @since 3.3.0
     */
    String resultMimeType() default MimeTypes.ANY;

    /**
     * SourceTypes define additional types that this transformer will accepts as a sourceType (beyond the method parameter).
     * At run time if the current message matches one of these source types, Mule will attempt to transform from
     * the source type to the method parameter type.  This means that transformations can be chained. The user can create
     * other transformers to be a part of this chain.
     *
     * @return an array of class types which allow the transformer to be matched on
     */
    Class[] sourceTypes() default {};
}
