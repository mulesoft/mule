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
package org.mule.jbi.engines.pxe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.mule.jbi.components.SimpleBootstrap;

/**
 * @author Propriétaire
 * 
 */
public class PxeBootstrap extends SimpleBootstrap {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.jbi.components.SimpleBootstrap#doInstall()
	 */
	protected void doInstall() throws Exception {
		String wksp = this.installContext.getContext().getWorkspaceRoot();
		copy(wksp, "hsqldb/pxeDb.script");
		copy(wksp, "hsqldb/pxeDb.properties");
	}

	private void copy(String rootDir, String file) throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(file);
		copy(url, new File(rootDir, file));

	}

	private int copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[8192];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	private void copy(URL url, File output) throws IOException {
		InputStream is = null;
		FileOutputStream os = null;
		try {
			// Copy url content stream to file
			is = url.openStream();
			output.getParentFile().mkdirs();
			os = new FileOutputStream(output);
			copy(is, os);
		} finally {
			closeQuietly(is);
			closeQuietly(os);
		}
	}

	private void closeQuietly(InputStream input) {
		try {
			if (input != null) {
				input.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}

	private void closeQuietly(OutputStream output) {
		try {
			if (output != null) {
				output.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}
}
