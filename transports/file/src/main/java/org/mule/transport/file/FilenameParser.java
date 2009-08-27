/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.MessageAdapter;

/**
 * <code>FilenameParser</code> is a simple expression parser interface for
 * processing filenames
 */
public interface FilenameParser extends MuleContextAware
{
    public String getFilename(MessageAdapter adapter, String pattern);
}
