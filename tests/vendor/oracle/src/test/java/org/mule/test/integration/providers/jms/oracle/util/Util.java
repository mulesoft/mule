package org.mule.test.integration.providers.jms.oracle.util;

import org.mule.util.ClassHelper;
import org.mule.util.Utility;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Miscellaneous convenience methods.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class Util {

    public static String getResourceAsString(String resource, Class callingClass) throws FileNotFoundException, MalformedURLException, IOException {
        InputStream is = ClassHelper.getResourceAsStream(resource, callingClass);
        if (is == null) {
            File file = new File(resource);
            if (file.exists()) {
            	is = new FileInputStream(file);
            } else {
                URL url = new URL(resource);
                is = url.openStream();
            }
        }
        return getInputStreamAsString(is);
    }

    /** Reads the input stream into a string. */
	public static String getInputStreamAsString(InputStream input) throws IOException {
        return (readToString(new InputStreamReader(input)));
    }

	/** Reads the stream into a string. */
	public static String readToString(Reader reader) throws IOException {
        String text = "";

        // Read the stream into an array of strings.
        ArrayList lines = readToArray(reader);

        // Concatenate the array of strings into a single string.
        int numLines = lines.size();
        for (int i = 0; i < numLines; ++i) {
            if (text.equals("") == false) {
            	text += Utility.CRLF;
            }
            text += lines.get(i);
        }
        return (text);
    }

    /** Reads the stream into an array of strings. */
	public static ArrayList readToArray(Reader reader) throws IOException {
	    ArrayList lines = new ArrayList();
        String line;
        BufferedReader buffer = new BufferedReader(reader);

        while ((line = buffer.readLine()) != null) {
            lines.add(line);
        }
        return (lines);
    }
	    
    public static Document stringToDom(String xml) throws SAXException, ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
}
