/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.util.Collection;

public interface ServerState
{

    void pushLastUpload(NamedPayload payload);

    NamedPayload getDownload(String name);

    Collection getDownloadNames();

    void started();

    void awaitStart(long ms) throws InterruptedException;

    NamedPayload awaitUpload(long ms) throws InterruptedException;

}
