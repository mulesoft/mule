/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

/**
 * A marker interface for exceptions thrown by message processors to perform "long returns".  They should not 
 * trigger exception strategy processing.
 */
public interface InternalProcessingException
{
}
