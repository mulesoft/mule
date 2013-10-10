/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
