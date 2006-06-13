/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.extras.pxe.transformers;

import java.io.File;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections.IteratorUtils;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JarToSystemDeploymentBundle extends AbstractSDBTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2967368724617938873L;

    public JarToSystemDeploymentBundle()
    {
        super();
        registerSourceType(File.class);
        registerSourceType(String.class);
    }

    protected Iterator getEntryIterator(Object src) throws Exception
    {
        ZipFile zip = null;
        if (src instanceof String)
        {
            zip = new ZipFile((String) src);
        }
        else
        {
            zip = new ZipFile((File) src);
        }

        return IteratorUtils.asIterator(zip.entries());
    }

    protected BundleEntry getBundleEntry(Object source, Object artifact)
            throws Exception
    {
        ZipEntry entry = (ZipEntry) artifact;
        File jar = null;
        if (source instanceof String)
        {
            jar = new File((String) source);
        }
        else
        {
            jar = (File) source;
        }
        return new BundleEntry(entry.getName(), "jar:"
                + jar.toURL().toExternalForm() + "!/" + entry.getName(), entry
                .isDirectory());
    }
}
