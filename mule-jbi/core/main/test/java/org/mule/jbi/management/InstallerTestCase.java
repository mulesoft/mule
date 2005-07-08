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
 */
package org.mule.jbi.management;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.InstallationContext;
import javax.management.ObjectName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.mule.jbi.JbiContainer;
import org.mule.jbi.util.IOUtils;
import org.w3c.dom.DocumentFragment;

import com.sun.java.xml.ns.jbi.JbiDocument;
import com.sun.java.xml.ns.jbi.JbiDocument.Jbi;

public class InstallerTestCase extends TestCase {

	public void setUp() throws Exception {
		IOUtils.deleteFile(new File("target/.mule-jbi"));
	}
	
	public void testInstallationDescriptorExtension() throws Exception {
		JbiContainer container = new JbiContainer();
		container.setWorkingDir(new File("target/.mule-jbi"));
		InputStream jbiIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-jbi-installation-descriptor.xml");
		Jbi jbi = JbiDocument.Factory.parse(jbiIs).getJbi();
		Installer installer = new TestInstaller(container, new File("target/.mule-jbi/cmp/testComponent"), jbi);
		installer.init();
	}
	
	public class TestInstaller extends Installer {

		public TestInstaller(JbiContainer container, File installRoot, Jbi jbi) {
			super(container, installRoot, jbi);
		}
		
		protected Bootstrap createBootstrap() throws Exception {
			return new Bootstrap() {
				public void init(InstallationContext installContext) throws JBIException {
					try {
						assertNotNull(installContext);
						DocumentFragment df = installContext.getInstallationDescriptorExtension();
						assertNotNull(df);
				        TransformerFactory factory = TransformerFactory.newInstance();
				        Transformer t = factory.newTransformer();
				        StringWriter buffer = new StringWriter();
				        t.transform(new DOMSource(df), new StreamResult(buffer));
				        String xml = buffer.toString();
				        System.err.println(xml);
					} catch (Exception e) {
						throw new JBIException(e);
					}
				}
				public void cleanUp() throws JBIException {
				}
				public ObjectName getExtensionMBeanName() {
					return null;
				}
				public void onInstall() throws JBIException {
				}
				public void onUninstall() throws JBIException {
				}
			};
		}
	}
	
}
