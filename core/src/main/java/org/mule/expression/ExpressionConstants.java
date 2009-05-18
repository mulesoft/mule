/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

/**
 * Constnts for Headers and Attachment processing shared by a number of the Expression evaluators
 */
public interface ExpressionConstants
{
    public static final String DELIM = ",";
    public static final String ALL_ARGUMENT = "*";
    public static final String COUNT_ARGUMENT = "{count}";
    public static final String OPTIONAL_ARGUMENT = "*"; //TODO this should be a '?' really...
}
