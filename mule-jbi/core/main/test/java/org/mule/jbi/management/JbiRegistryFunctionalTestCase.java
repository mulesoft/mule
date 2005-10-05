/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.management;

import org.mule.jbi.AbstractFunctionalTestCase;
import org.mule.jbi.JbiContainer;

import java.util.Locale;

public class JbiRegistryFunctionalTestCase extends AbstractFunctionalTestCase {

	public void testRealInstall() throws Exception {
		Locale.setDefault(Locale.US);
		installLibrary("wsdlsl.jar");
		installComponent("filebinding.jar");
		container.shutDown();
		container.start();
        assertNotNull(container.getRegistry().getLibrary("SunWSDLSharedLibrary"));
        assertNotNull(container.getRegistry().getComponent("SunFileBinding"));
		//installComponent("transformationengine.jar");
		//deployAssembly("sa.zip");
		//installComponent("soapbinding.jar");
		//installComponent("sequencingengine.jar");
	}
}
