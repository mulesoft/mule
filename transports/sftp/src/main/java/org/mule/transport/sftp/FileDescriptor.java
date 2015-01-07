/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.SftpATTRS;

/** Describing a file by it's filename and it's attributes.
 *
 * Created by christianlangmann on 06/01/15.
 */
public class FileDescriptor {
    private final String filename;

    private final SftpATTRS attrs;

    public FileDescriptor(final String filename, final SftpATTRS descriptor) {
        this.filename = filename;
        this.attrs = descriptor;
    }

    public String getFilename() {
        return filename;
    }

    public SftpATTRS getAttrs() {
        return attrs;
    }
}
