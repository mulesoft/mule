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
import org.mule.registry.RegistryComponent;
import org.mule.registry.RegistryDescriptor;
import org.mule.registry.RegistryException;
import org.mule.registry.ValidationException;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiDescriptor implements RegistryDescriptor {

    public static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";

	protected String installRoot;
    protected JbiDocument.Jbi configuration = null;

    public JbiDescriptor(JbiDocument.Jbi configuration) throws RegistryException {
//        if (this.installRoot == null) {
//            throw new IllegalStateException("installRoot must be set");
//        }
//        this.installRoot = installRoot;
//        
        this.configuration = configuration;
        init();
    }

    public JbiDescriptor(String installRoot) throws RegistryException {
        if (installRoot == null) {
            throw new IllegalStateException("installRoot must be set");
        }
        this.installRoot = installRoot;
        init();
    }

    public RegistryComponent getComponent() {
        return null;
    }

    public boolean isServiceAssembly() {
        return configuration.isSetServiceAssembly();
    }

    public boolean isComponent() {
        return configuration.isSetComponent();
    }

    public boolean isSharedLibrary() {
        return configuration.isSetSharedLibrary();
    }

    public boolean isServices() {
        return configuration.isSetServices();
    }

    public Object getConfiguration() {
        return configuration;
    }

	protected void init() throws RegistryException {
		try {
			if (this.configuration == null) {
				File file = new File(this.installRoot, JBI_DESCRIPTOR);
				if (!file.isFile()) {
					throw new FileNotFoundException(file.getAbsolutePath());
				}
				this.configuration = JbiDocument.Factory.parse(file).getJbi();
				validate();
			}
        } catch (Exception e) {
			throw new RegistryException(e);
		}
	}

    public void validate() throws ValidationException {
        // Check version number
		if (this.configuration.getVersion().doubleValue() != 1.0) {
			throw new ValidationException("version attribute should be '1.0'");
		}
    }
}
