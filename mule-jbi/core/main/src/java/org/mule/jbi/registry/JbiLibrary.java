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
package org.mule.jbi.registry;

import com.sun.java.xml.ns.jbi.JbiDocument;
import org.mule.registry.Registry;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.impl.AbstractLibrary;

import java.util.Arrays;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiLibrary extends AbstractLibrary {

    public JbiLibrary(Registry registry) {
        super(registry);
    }

    /**
     * Return the descriptor for this component.
     *
     * @return
     */
    public RegistryDescriptor getDescriptor() throws RegistryException {
        if(descriptor==null) {
            descriptor = new JbiDescriptor(this.getInstallRoot());
        }
        return descriptor;
    }

    protected void doInstall() throws Exception {
        JbiDocument.Jbi jbi = (JbiDocument.Jbi)getDescriptor().getConfiguration();
        // Get class path elements
		this.classPathElements = Arrays.asList(jbi.getSharedLibrary().getSharedLibraryClassPath().getPathElementArray());
		// Class loader delegation
		this.isClassLoaderParentFirst = com.sun.java.xml.ns.jbi.JbiDocument.Jbi.SharedLibrary.ClassLoaderDelegation.PARENT_FIRST.equals(jbi.getSharedLibrary().getClassLoaderDelegation());
    }

    protected void doUninstall() throws Exception {

    }
}
