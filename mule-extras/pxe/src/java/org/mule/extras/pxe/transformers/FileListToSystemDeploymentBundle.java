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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.transformer.TransformerException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileListToSystemDeploymentBundle extends DirectoryToSystemDeploymentBundle {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public FileListToSystemDeploymentBundle() {
        super();
        registerSourceType(List.class);
        registerSourceType(String[].class);
        registerSourceType(File[].class);
    }

    protected Iterator getEntryIterator(Object src) throws FileNotFoundException, TransformerException {
        if (src instanceof List) {
            src = ((List) src).toArray();
        }

        if (src instanceof File[]) {
            return Arrays.asList((File[]) src).iterator();
        } else if (src instanceof String[]) {
            String[] array = (String[]) src;
            List files = new ArrayList(array.length);
            for (int i = 0; i < array.length; i++) {
                String s = array[i];
                File f = new File(s);
                if (f.exists()) {
                    files.add(f);
                } else {
                    throw new FileNotFoundException(s);
                }
            }
            return files.iterator();
        } else {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X, src.getClass().getName()), this);
        }
    }
}
