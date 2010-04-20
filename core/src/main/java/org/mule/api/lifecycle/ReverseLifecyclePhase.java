/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * A marker interface that makes a lifecycle transition revert to the state previous to the phase execution rather than
 * progressing the lifecycle to a new phase.
 */
public interface ReverseLifecyclePhase
{
}
