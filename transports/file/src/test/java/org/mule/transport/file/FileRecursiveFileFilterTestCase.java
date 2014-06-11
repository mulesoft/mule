/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import java.io.File;
import java.io.FileFilter;

public class FileRecursiveFileFilterTestCase extends AbstractFileRecursiveFilterTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "file-recursive-file-filter-config.xml";
    }

    public static class TxtFileFilter implements Filter,FileFilter
    {

        public boolean accept(MuleMessage message)
        {
            String filename = message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME);
            return acceptsFile(filename);
        }

        public boolean accept(File file)
        {
            return acceptsFile(file.getName());
        }

        private boolean acceptsFile(String filename)
        {
            return filename.endsWith(".txt");
        }
    }
}
