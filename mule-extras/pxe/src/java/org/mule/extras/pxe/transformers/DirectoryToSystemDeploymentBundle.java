/* 
* $Header$
* $Revision$
* $Date$
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

import org.mule.util.ClassHelper;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DirectoryToSystemDeploymentBundle extends AbstractSDBTransformer
{
    public DirectoryToSystemDeploymentBundle() {
        super();
        registerSourceType(File.class);
        registerSourceType(String.class);
    }

    protected Iterator getEntryIterator(Object src) throws Exception {
        File f;
        if(src instanceof String) {
            f = new File(src.toString());
            if(!f.exists()) {
                URL url = ClassHelper.getResource(src.toString(), getClass());
                f = new File(new URI(url.toString()));
                return Arrays.asList(f.listFiles()).iterator();
            }

        }
        f = (File)src;
        return Arrays.asList(f.listFiles()).iterator();
    }

    protected BundleEntry getBundleEntry(Object source, Object artifact) throws Exception {
        File  f = (File)artifact;
        return new BundleEntry(f.getName(), f.toURL().toString(), f.isDirectory());
    }
}
