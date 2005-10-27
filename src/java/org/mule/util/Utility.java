/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/util/Utility.java,v 1.13
 * 2004/01/14 09:34:02 rossmason Exp $ $Revision$ $Date: 2004/01/14
 * 09:34:02 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */
package org.mule.util;

import org.mule.MuleManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <code>Utility</code> is a singleton grouping common functionality like
 * converting java.lang.String to different data types, reading files, etc
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Utility
{
    /**
     * The current platform's CR/LF delimiter
     */
    public static final String CRLF = System.getProperty("line.separator");

    /**
     * Saves creation of unecessary string creation
     */
    public static final String EMPTY_STRING = "";

    /**
     * The default buffer size to use.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static File createFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.canWrite()) {
            String dirName = file.getPath();
            int i = dirName.lastIndexOf(File.separator);
            if (i > -1) {
                dirName = dirName.substring(0, i);
                File dir = new File(dirName);
                dir.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

    public static String prepareWinFilename(String filename) {
        filename = filename.replaceAll("<", "(");
        filename = filename.replaceAll(">", ")");
        filename = filename.replaceAll("[/\\*?|:;]", "-");
        return filename;
    }

    public static File openDirectory(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.isDirectory() || !dir.canRead()) {
            throw new IOException("Directory: " + directory + " exists but isn't a directory");
        }
        return dir;
    }

    /**
     * Allocates a Boolean object representing the value true if the string
     * argument is not null and is equal, ignoring case, to the string "true".
     * Otherwise, throws an exception.
     */
    public static boolean getBooleanValue(String s) throws Exception {
        boolean result;
        s = s.trim();
        if (s.equalsIgnoreCase("true")) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public static String getTimeStamp(String format) {

        // Format the current time.
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    public static String formatTimeStamp(Date dateTime, String format) {
        // Format the current time.
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(dateTime);
    }

    /**
     * Reads the incoming file and returns the content as a String object.
     */
    public static synchronized String fileToString(String fileName) throws IOException {
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[1024 * 8];

        FileReader fr = new FileReader(fileName);
        int read = 0;
        while ((read = fr.read(buf)) >= 0) {
            sb.append(buf, 0, read);
        }

        return sb.toString();
    }

    /**
     * Reads the incoming String into a file at at the given destination.
     *
     * @param filename name and path of the file to create
     * @param data     the contents of the file
     * @return the new file.
     * @throws IOException If the creating or writing to the file stream fails
     */
    public static File stringToFile(String filename, String data) throws IOException {
        return stringToFile(filename, data, false);
    }

    public static synchronized File stringToFile(String filename, String data, boolean append) throws IOException {
        return stringToFile(filename, data, append, false);
    }

    public static synchronized File stringToFile(String filename, String data, boolean append, boolean newLine)
            throws IOException {
        File f = createFile(filename);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f, append));
            writer.write(data);
            if (newLine) {
                writer.newLine();
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return f;
    }

    public static String getStringFromDate(Date date, String format) {
        // converts from date to strin using the standard TIME_STAMP_FORMAT
        // pattern
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static Date getDateFromString(String date, String format) {
        // The date must always be in the format of TIME_STAMP_FORMAT
        // i.e. JAN 29 2001 22:50:40 GMT
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        ParsePosition pos = new ParsePosition(0);

        // Parse the string back into a Time Stamp.
        return formatter.parse(date, pos);
    }

    public static File loadFile(String filename) throws IOException {
        File file = new File(filename);
        if (file.canRead()) {
            return file;
        } else {
            throw new IOException("File: " + filename + " can not be read");
        }
    }

    public static byte[] objectToByteArray(Object src) throws IOException {
        if (src instanceof byte[]) {
            return (byte[]) src;
        } else if (src instanceof String) {
            return ((String) src).getBytes();
        }

        byte[] dest = null;
        ByteArrayOutputStream bs = null;
        ObjectOutputStream os = null;
        try {
            bs = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bs);
            os.writeObject(src);
            os.flush();
            dest = bs.toByteArray();
        } catch (IOException e) {
            os.close();
            bs.close();
            throw e;
        }
        return dest;
    }

    public static Object byteArrayToObject(byte[] src) throws IOException, ClassNotFoundException {
        Object dest = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(src);
        ObjectInputStream ois = new ObjectInputStream(bais);
        dest = ois.readObject();
        ois.close();
        return dest;
    }

    /**
     * Load a given resource. Trying broader class loaders each time.
     *
     * @param resourceName The name of the resource to load
     * @param callingClass The Class object of the calling object
     */
    public static URL getResource(final String resourceName, final Class callingClass) {
        URL url = ClassHelper.getResource(resourceName, callingClass);


        if (url == null) {
            url = (URL) AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    File f = new File(resourceName);
                    if (f.exists()) {
                        try {
                            return f.toURL();
                        } catch (MalformedURLException e) {
                            return null;
                        }
                    }
                    return null;
                }
            });
        }
        return url;
    }

    public static String loadResourceAsString(String resourceName, Class callingClass) throws IOException {
        return loadResourceAsString(resourceName, callingClass, MuleManager.getConfiguration().getEncoding());
    }
    
    public static String loadResourceAsString(String resourceName, Class callingClass, String encoding) throws IOException {
        URL url = getResource(resourceName, callingClass);
        String resource = null;
        if (url == null) {
            resource = fileToString(resourceName);
        } else {
            resource = fileToString(URLDecoder.decode(url.getFile(), encoding));
        }
        return resource;
    }

    public static InputStream loadResource(String resourceName, Class callingClass) throws IOException {
        URL url = getResource(resourceName, callingClass);
        InputStream resource = null;
        if (url == null) {
            File f = new File(resourceName);
            if (f.exists()) {
                resource = new FileInputStream(f);
            }
        } else {
            resource = url.openStream();
        }
        return resource;
    }

    public static String getResourcePath(String resourceName, Class callingClass) throws IOException {
        return getResourcePath(resourceName, callingClass, MuleManager.getConfiguration().getEncoding());
    }

    public static String getResourcePath(String resourceName, Class callingClass, String encoding) throws IOException {
        if (resourceName == null) {
            return null;
        }
        URL url = getResource(resourceName, callingClass);
        String resource = null;
        if (url == null) {
            File f = new File(resourceName);
            if (f.exists()) {
                resource = f.getAbsolutePath();
            }
        } else {
            resource = URLDecoder.decode(url.toExternalForm(), encoding);
        }
        if (resource != null) {
            if (resource.startsWith("file:/")) {
                resource = resource.substring(6);
            }
            if (!resource.startsWith(File.separator)) {
                resource = File.separator + resource;
            }
        }

        return resource;
    }

    public static boolean deleteTree(File dir) {
        if (dir == null) {
            return true;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (!deleteTree(files[i])) {
                    return false;
                }
            } else {
                if (!files[i].delete()) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static String[] split(String string, String delim) {
        StringTokenizer st = new StringTokenizer(string, delim);
        String[] results = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            results[i++] = st.nextToken().trim();
        }
        return results;
    }

    public static String getFormattedDuration(long mills) {
        long days = mills / 86400000;
        mills = mills - (days * 86400000);
        long hours = mills / 3600000;
        mills = mills - (hours * 3600000);
        long mins = mills / 60000;
        mills = mills - (mins * 60000);
        long secs = mills / 1000;
        mills = mills - (secs * 1000);

        StringBuffer bf = new StringBuffer();
        bf.append(days).append(" days, ");
        bf.append(hours).append(" hours, ");
        bf.append(mins).append(" mins, ");
        bf.append(secs).append(".").append(mills).append(" sec");
        return bf.toString();
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
			copy(is, os);
		} finally {
			closeQuietly(is);
			closeQuietly(os);
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
}
