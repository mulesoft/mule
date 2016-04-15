/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import org.mule.module.db.internal.domain.query.QueryTemplate;

/**
 * Creates {@link ParamTypeResolver} instances
 */
public interface ParamTypeResolverFactory
{

    /**
     * Creates a new parameter type resolver to resolve the parameter types
     * of a given query template.
     *
     * @param queryTemplate query template to resolve
     * @return a non null {@link ParamTypeResolver} to resolve the given query template
     */
    ParamTypeResolver create(QueryTemplate queryTemplate);
}
