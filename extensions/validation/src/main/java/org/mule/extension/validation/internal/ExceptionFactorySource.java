/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;


import org.mule.extension.annotations.Alias;
import org.mule.extension.validation.api.ExceptionFactory;

/**
 * A {@link ObjectSource} for instance of {@link ExceptionFactory}.
 * The reason why this is a top level class instead of just an instance
 * is to hint the extensions api with an {@link Alias} annotation
 *
 * @since 3.7.0
 */
@Alias("exceptionFactory")
public class ExceptionFactorySource extends ObjectSource<ExceptionFactory>
{

}
