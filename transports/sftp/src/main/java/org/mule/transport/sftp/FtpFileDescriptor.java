/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.SftpATTRS;

/**
 * Created by christianlangmann on 06/01/15.
 */
public class FtpFileDescriptor {
    private final String filename;

    private final SftpATTRS descriptor;

    public FtpFileDescriptor(final String filename, final SftpATTRS descriptor) {
        this.filename = filename;
        this.descriptor = descriptor;
    }

    public String getFilename() {
        return filename;
    }

    public SftpATTRS getDescriptor() {
        return descriptor;
    }
}
