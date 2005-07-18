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
package org.mule.jbi.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class IOUtils {

    /**
     * The default buffer size to use.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    // copy from InputStream
    //-----------------------------------------------------------------------
    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since 1.1
     */
    public static int copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    
    public static void copy(URL url, File output) throws IOException {
		InputStream is = null;
		FileOutputStream os = null;
		try {
			// Copy url content stream to file
			is = url.openStream();
			os = new FileOutputStream(output);
			IOUtils.copy(is, os);
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
    }
    
    /**
     * Unconditionally close an <code>InputStream</code>.
     * <p>
     * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param input  the InputStream to close, may be null or already closed
     */
    public static void closeQuietly(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * Unconditionally close an <code>OutputStream</code>.
     * <p>
     * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param output  the OutputStream to close, may be null or already closed
     */
    public static void closeQuietly(OutputStream output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
    
    /**
     * Unzip the specified archive to the given directory
     */
    public static void unzip(File archive, File directory) throws IOException {
    	ZipFile zip = null;
    	if (directory.exists()) {
    		if (!directory.isDirectory()) {
    			throw new IOException("Directory is not a directory: " + directory);
    		}
    	} else {
    		if (!directory.mkdirs()) {
    			throw new IOException("Could not create directory: " + directory);
    		}
    	}
    	try {
    		zip = new ZipFile(archive);
    		for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
    			ZipEntry entry = (ZipEntry) entries.nextElement();
    			File f = new File(directory, entry.getName());
				if (entry.isDirectory()) {
					if (!f.mkdirs()) {
						throw new IOException("Could not create directory: " + f);
					}
				} else {
					InputStream is = zip.getInputStream(entry);
					OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
					copy(is, os);
					is.close();
					os.close();
				}
			}
    	} finally {
    		if (zip != null) {
    			zip.close();
    		}
    	}
    }
    
    public static void createDirs(File dir) throws IOException {
    	dir.mkdirs();
    	if (!dir.isDirectory()) {
    		throw new IOException("Could not create directory: " + dir);
    	}
    }
    
    public static void deleteFile(File dir) {
    	if (dir != null && dir.exists()) {
	    	if (dir.isDirectory()) {
	    		File[] children = dir.listFiles();
	    		for (int i = 0; i < children.length; i++) {
		    		deleteFile(children[i]);
				}
	    	}
			dir.delete();
    	}    	
    }

}
