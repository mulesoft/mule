/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for in memory implementation of {@link ResultSet} for testing purpose.
 * <p/>
 * NOTE: partial implementation to cover current test scenarios
 */
public abstract class AbstractInMemoryResultSet implements ResultSet
{

    private final Map<String, ColumnMetadata> columnsByName = new HashMap<String, ColumnMetadata>();
    private final Map<Integer, ColumnMetadata> columnsByIndex = new HashMap<Integer, ColumnMetadata>();
    private List<ColumnMetadata> columns;
    private final List<Map<String, Object>> records;
    private final Statement statement;

    private int currentColumnIndex = 0;
    private boolean closed;
    private boolean wasNull;

    /**
     * Creates a new in memory resultSet
     *
     * @param columns column definitions
     * @param records values for each record using the order defined in the columns
     */
    public AbstractInMemoryResultSet(List<ColumnMetadata> columns, List<Map<String, Object>> records, Statement statement)
    {
        this.columns = columns;
        this.records = records;
        this.statement = statement;

        for (ColumnMetadata column : columns)
        {
            columnsByIndex.put(column.getIndex(), column);
            columnsByName.put(column.getName(), column);
        }
    }


    @Override
    public boolean next() throws SQLException
    {
        checkOpenResultSet();
        return currentColumnIndex++ < records.size();
    }

    @Override
    public void close() throws SQLException
    {
        closed = true;
    }

    @Override
    public boolean wasNull() throws SQLException
    {
        checkOpenResultSet();

        return wasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException
    {
        return (String) getColumnValue(columnIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException
    {
        return (Boolean) getColumnValue(columnIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException
    {
        return (Byte) getColumnValue(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException
    {
        return (Short) getColumnValue(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException
    {
        return (Integer) getColumnValue(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException
    {
        return (Long) getColumnValue(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException
    {
        return (Float) getColumnValue(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException
    {
        return (Double) getColumnValue(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int i2) throws SQLException
    {
        return (BigDecimal) getColumnValue(columnIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException
    {
        return (byte[]) getColumnValue(columnIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException
    {
        return (Date) getColumnValue(columnIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException
    {
        return (Time) getColumnValue(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException
    {
        return (Timestamp) getColumnValue(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException
    {
        return (InputStream) getColumnValue(columnIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException
    {
        return (InputStream) getColumnValue(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException
    {
        return (InputStream) getColumnValue(columnIndex);
    }

    @Override
    public String getString(String columnLabel) throws SQLException
    {
        return (String) getColumnValue(columnLabel);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException
    {
        return (Boolean) getColumnValue(columnLabel);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException
    {
        return (Byte) getColumnValue(columnLabel);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException
    {
        return (Short) getColumnValue(columnLabel);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException
    {
        return (Integer) getColumnValue(columnLabel);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException
    {
        return (Long) getColumnValue(columnLabel);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException
    {
        return (Float) getColumnValue(columnLabel);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException
    {
        return (Double) getColumnValue(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int i) throws SQLException
    {
        return (BigDecimal) getColumnValue(columnLabel);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException
    {
        return (byte[]) getColumnValue(columnLabel);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException
    {
        return (Date) getColumnValue(columnLabel);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException
    {
        return (Time) getColumnValue(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException
    {
        return (Timestamp) getColumnValue(columnLabel);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException
    {
        return (InputStream) getColumnValue(columnLabel);
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException
    {
        return (InputStream) getColumnValue(columnLabel);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException
    {
        return (InputStream) getColumnValue(columnLabel);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        checkOpenResultSet();
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        checkOpenResultSet();
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCursorName() throws SQLException
    {
        checkOpenResultSet();
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException
    {
        checkOpenResultSet();

        return new TestResultSetMetaData(columns);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException
    {
        return getColumnValue(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException
    {
        return getColumnValue(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException
    {
        checkOpenResultSet();

        ColumnMetadata columnMetadata = columnsByName.get(columnLabel);
        return columnMetadata.getIndex();
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException
    {
        return (Reader) getColumnValue(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException
    {
        return (Reader) getColumnValue(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException
    {
        return (BigDecimal) getColumnValue(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException
    {
        return (BigDecimal) getColumnValue(columnLabel);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAfterLast() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFirst() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLast() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeFirst() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void afterLast() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean first() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean last() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean absolute(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean relative(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean previous() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getFetchDirection() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void setFetchDirection(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getFetchSize() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void setFetchSize(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getConcurrency() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowUpdated() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowInserted() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowDeleted() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBoolean(int i, boolean b) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateByte(int i, byte b) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShort(int i, short i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateInt(int i, int i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLong(int i, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFloat(int i, float v) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(int i, double v) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateString(int i, String s) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(int i, byte[] bytes) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(int i, Date date) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTime(int i, Time time) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, int i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, int i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, int i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(int i, Object o, int i2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(int i, Object o) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBoolean(String s, boolean b) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateByte(String s, byte b) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShort(String s, short i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateInt(String s, int i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLong(String s, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFloat(String s, float v) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(String s, double v) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateString(String s, String s2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(String s, byte[] bytes) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(String s, Date date) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTime(String s, Time time) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(String s, Object o, int i) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(String s, Object o) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void insertRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelRowUpdates() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToInsertRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToCurrentRow() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Statement getStatement() throws SQLException
    {
        checkOpenResultSet();

        return statement;
    }

    @Override
    public Object getObject(int i, Map<String, Class<?>> stringClassMap) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException
    {
        return (Ref) getColumnValue(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException
    {
        return (Blob) getColumnValue(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException
    {
        return (Clob) getColumnValue(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException
    {
        return (Array) getColumnValue(columnIndex);
    }

    @Override
    public Object getObject(String s, Map<String, Class<?>> stringClassMap) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException
    {
        return (Ref) getColumnValue(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException
    {
        return (Blob) getColumnValue(columnLabel);
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException
    {
        return (Clob) getColumnValue(columnLabel);
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException
    {
        return (Array) getColumnValue(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(String columnLabel, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(int columnIndex, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Time getTime(String columnLabel, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar calendar) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException
    {
        return (URL) getColumnValue(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException
    {
        return (URL) getColumnValue(columnLabel);
    }

    @Override
    public void updateRef(int i, Ref ref) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRef(String s, Ref ref) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int i, Blob blob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String s, Blob blob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int i, Clob clob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String s, Clob clob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArray(int i, Array array) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateArray(String s, Array array) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException
    {
        return (RowId) getColumnValue(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException
    {
        return (RowId) getColumnValue(columnLabel);
    }

    @Override
    public void updateRowId(int i, RowId rowId) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRowId(String s, RowId rowId) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public int getHoldability() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNString(int i, String s) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNString(String s, String s2) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int i, NClob nClob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String s, NClob nClob) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException
    {
        return (NClob) getColumnValue(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException
    {
        return (NClob) getColumnValue(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException
    {
        return (SQLXML) getColumnValue(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException
    {
        return (SQLXML) getColumnValue(columnLabel);
    }

    @Override
    public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public String getNString(int columnIndex) throws SQLException
    {
        return (String) getColumnValue(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException
    {
        return (String) getColumnValue(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException
    {
        return (Reader) getColumnValue(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException
    {
        return (Reader) getColumnValue(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int i, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String s, InputStream inputStream, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int i, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String s, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int i, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String s, Reader reader, long l) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int i, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(String s, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(int i, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBlob(String s, InputStream inputStream) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(int i, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateClob(String s, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(int i, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNClob(String s, Reader reader) throws SQLException
    {
        checkOpenResultSet();

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> tClass) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> aClass) throws SQLException
    {
        throw new UnsupportedOperationException();
    }


    protected void checkOpenResultSet() throws SQLException
    {
        if (closed)
        {
            throw new SQLException("ResultSet is already closed");
        }
    }

    protected Object getColumnValue(int columnIndex) throws SQLException
    {
        checkOpenResultSet();

        ColumnMetadata columnMetadata = columnsByIndex.get(columnIndex);
        Map<String, Object> record = records.get(currentColumnIndex - 1);

        Object value = record.get(columnMetadata.getName());
        wasNull = value == null;
        return value;
    }

    protected Object getColumnValue(String columnLabel) throws SQLException
    {
        checkOpenResultSet();

        Map<String, Object> record = records.get(currentColumnIndex - 1);

        Object value = record.get(columnLabel);
        wasNull = value == null;
        return value;
    }
}