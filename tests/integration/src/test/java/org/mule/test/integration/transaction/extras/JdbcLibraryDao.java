/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction.extras;

import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcLibraryDao implements LibraryDao
{

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertBook(Book book) throws Exception
    {
        String sql = "insert into book (id, title, author) values (?,?,?)";
        Object args[] = new Object[] {new Integer(book.getSerialNo()), book.getTitle(), book.getAuthor()};
        int types[] = new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR};
        try
        {
            jdbcTemplate.update(sql, args, types);
            return true;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            throw e;
            //return false;
        }
    }
}