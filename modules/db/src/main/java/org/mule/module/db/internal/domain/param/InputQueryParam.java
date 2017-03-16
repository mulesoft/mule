/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

/**
 * Defines a parameter that provides an input value
 */
public interface InputQueryParam extends QueryParam
{

    Object getValue();

    /**
     * @return true if the parameter has a defined value in the query template, false otherwise.
     */
    boolean hasValue();

}
