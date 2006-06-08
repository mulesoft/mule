/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.dq;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CharacterFieldDescription;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

/**
 * @author m999svm <p/> DQMessageUtils utility class for DQMessage
 */
public final class DQMessageUtils
{
    /**
     * Logger for this class
     */
    private static Log log = LogFactory.getLog(DQMessageUtils.class);

    public static final String RECORD_DESCRIPTOR_KEY = "recordDescriptor";

    /**
     * The constructor
     */
    private DQMessageUtils()
    {
        super();
    }

    /**
     * Returns a DQMessage corresponding to the byte array and the record format
     * 
     * @param data The data
     * @param format The record format
     * @return The DQMessage
     * @throws Exception Error during the data processing
     */
    public static synchronized DQMessage getDQMessage(final byte[] data, final RecordFormat format) throws Exception
    {
        Record record = format.getNewRecord(data);

        DQMessage message = new DQMessage();

        String[] names = format.getFieldNames();
        String name;
        for (int i = 0; i < names.length; i++) {
            name = names[i];
            message.addEntry(name, record.getField(name));
        }

        return message;
    }

    /**
     * Returns a Record corresponding to the DQMessage and the record format
     * 
     * @param msg The DQMessage
     * @param format The record format
     * @return The Record
     */

    public static synchronized Record getRecord(final DQMessage msg, final RecordFormat format)
    {
        Record rec = format.getNewRecord();

        String name;
        Object field;
        String[] tab = format.getFieldNames();

        for (int i = 0; i < tab.length; i++) {
            name = tab[i];
            field = msg.getEntry(name);
            if (field == null) {
                field = "";
            }
            rec.setField(name, field);
        }

        return rec;
    }

    /**
     * Returns the record format described in the record descriptor file.
     * 
     * @param recordDescriptor The record descriptor filename
     * @param as400 The as400
     * @return The RecordFormat
     * @throws Exception Error during the record format parsing
     */
    public static synchronized RecordFormat getRecordFormat(final String recordDescriptor, final AS400 as400)
            throws Exception
    {
        log.debug("Record descriptor :" + recordDescriptor);

        if (recordDescriptor == null) {
            throw new Exception("Failed to read record descriptor : recordDescriptor property is not set ");
        }
        
        InputStream input = null;
        RecordFormat recordFormat = null;

        try {
            File file = new File(recordDescriptor);

            if (file.exists()) {
                input = new FileInputStream(file);
            } else {
                input = DQMessageUtils.class.getClassLoader().getResourceAsStream(recordDescriptor);
                if (input == null) {
                    throw new Exception("Failed to read record descriptor  (" + recordDescriptor + ") cannot be found ");
                }
            }

            recordFormat = parse(input, as400);
        } catch (Exception e) {
            throw e;
        } finally {
            if (input != null) {
                input.close();
            }
        }

        return recordFormat;
    }

    /**
     * Returns the record format.
     * 
     * @param stream The record descriptor inputstream
     * @param as400 The as400
     * @return The RecordFormat
     * @throws Exception Error during the record format parsing
     */
    private static RecordFormat parse(final InputStream stream, final AS400 as400) throws Exception
    {

        SAXReader reader = new SAXReader();
        Document document = reader.read(stream);

        RecordFormat rec = new RecordFormat();
        Element root = document.getRootElement();
        String name;
        int length;
        Element element;

        for (Iterator i = root.elementIterator(); i.hasNext();) {
            element = (Element) i.next();
            name = element.attributeValue("name");
            length = Integer.decode(element.attributeValue("length")).intValue();
            addCharacterField(rec, length, name, as400);
        }

        return rec;
    }

    /**
     * Add a charachter field in the record format.
     * 
     * @param format The record format
     * @param length The field length
     * @param as400 The as400
     * @param name The field name
     */
    private static void addCharacterField(final RecordFormat format,
                                          final int length,
                                          final String name,
                                          final AS400 as400)
    {
        format.addFieldDescription(new CharacterFieldDescription(new AS400Text(length, as400), name));
    }

}
