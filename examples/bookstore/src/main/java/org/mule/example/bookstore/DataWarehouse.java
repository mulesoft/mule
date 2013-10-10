/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.bookstore;

import org.mule.example.bookstore.web.HtmlTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tracks statistics on book orders.
 */
public class DataWarehouse
{
    int booksOrdered = 0;
    double totalRevenue = 0;
    double averagePrice = 0;
    Map <String, Integer> sales = new HashMap <String, Integer> ();
    String bestSeller = "";
    
    public String updateStats(Order order)
    {
        Book book = order.getBook();
        booksOrdered += order.getQuantity();
        totalRevenue += (book.getPrice() * order.getQuantity());
        averagePrice = totalRevenue / booksOrdered;

        String title = book.getTitle();
        Integer quantity = sales.get(title);
        if (quantity == null)
        {
            sales.put(title, order.getQuantity());
        }
        else
        {
            sales.put(title, quantity + order.getQuantity());
        }
        bestSeller = getBestSeller();
        
        System.out.println("Updating stats");
        return HtmlTemplate.wrapHtmlBody(printHtmlStats());
    }

    protected String getBestSeller()
    {
        String title = "";
        int quantity = 0;
        for (Entry<String, Integer> entry : sales.entrySet())
        {
            if (entry.getValue() > quantity)
            {
                title = entry.getKey();
                quantity = entry.getValue();
            }
        }
        return title;
    }
    
    protected String printHtmlStats()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Data Warehouse Statistics</h2>");
        sb.append("<table>");
        sb.append("  <tr><th>Books sold</th> <td>").append(booksOrdered).append("</td></tr>");
        sb.append(String.format("  <tr><th>Total revenue</th> <td>$%.2f</td></tr>", totalRevenue));
        sb.append(String.format("  <tr><th>Average price</th> <td>$%.2f</td></tr>", averagePrice));
        sb.append("<tr><th>Best seller</th> <td>").append(bestSeller).append("</td></tr>");
        sb.append("</table>");

        return sb.toString();
    }
}
