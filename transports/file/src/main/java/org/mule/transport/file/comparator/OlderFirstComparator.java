/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.comparator;

import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.Comparator;

/**
 * <p><code>OlderComparatorComparator</code> is a {@link Comparator} of File
 * which is capable of comparing files for equality based on their modification dates.</p>
 */
public class OlderFirstComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        if (o1 instanceof File && o2 instanceof File)
        {
            File f = (File) o1;
            File f1 = (File) o2;
            boolean fileNewer = FileUtils.isFileNewer(f, f1);
            boolean fileOlder = FileUtils.isFileOlder(f, f1);
            if (!fileNewer && !fileOlder)
            {
                return 0;
            }
            else if (fileNewer)
            {
                return 1;
            }
            else
            {
                return -1;
            }

        }
        throw new IllegalArgumentException(MessageFormat.format(
                "Expected java.io.File instance, but was {0} and {1}",
                ClassUtils.getShortClassName(o1, "<null>"),
                ClassUtils.getShortClassName(o2, "<null")));
    }
}
