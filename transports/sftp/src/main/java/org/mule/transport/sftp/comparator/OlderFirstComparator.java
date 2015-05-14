/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.comparator;

import org.mule.transport.sftp.FileDescriptor;
import org.mule.util.ClassUtils;

import java.text.MessageFormat;
import java.util.Comparator;
import com.jcraft.jsch.SftpATTRS;
import org.mule.transport.sftp.FileDescriptor;
import org.mule.util.ClassUtils;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;

/**
 * <p><code>OlderComparatorComparator</code> is a {@link Comparator} of SFTP-files
 * which is capable of comparing files for equality based on their modification dates.</p>
 */
public class OlderFirstComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        if (o1 instanceof FileDescriptor && o2 instanceof FileDescriptor) {
            final FileDescriptor f = (FileDescriptor) o1;
            final FileDescriptor f1 = (FileDescriptor) o2;
            boolean fileNewer = f.getAttrs().getMTime() > f1.getAttrs().getMTime();
            boolean fileOlder = f.getAttrs().getMTime() < f1.getAttrs().getMTime();
            if (!fileNewer && !fileOlder) {
                return 0;
            } else if (fileNewer) {
                return 1;
            } else {
                return -1;
            }

        }
        throw new IllegalArgumentException(MessageFormat.format(
                "Expected FileDescriptor instance, but was {0} and {1}",
                ClassUtils.getShortClassName(o1, "<null>"),
                ClassUtils.getShortClassName(o2, "<null")));
    }
}
