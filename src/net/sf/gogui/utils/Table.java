//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

//----------------------------------------------------------------------------

public class Table
{
    public Table()
    {
        m_columnTitles = new Vector();
        m_numberColumns = 0;
    }

    public Table(Vector columnTitles)
    {        
        m_columnTitles = columnTitles;
        m_numberColumns = columnTitles.size();
    }

    public String get(int column, int row)
    {
        return (String)getRow(row).get(column);
    }

    public String get(String columnTitle, int row)
    {
        return get(getColumnIndex(columnTitle), row);
    }

    public int getColumnIndex(String column)
    {
        for (int i = 0; i < m_numberColumns; ++i)
        {
            String title = getColumnTitle(i);
            if (title.equals(column))
                return i;
        }
        assert(false);
        return -1;
    }

    public String getColumnTitle(int index)
    {
        return (String)m_columnTitles.get(index);
    }

    public Vector getColumnTitles()
    {
        return (Vector)m_columnTitles.clone();
    }

    public int getNumberColumns()
    {
        return m_columnTitles.size();
    }

    public int getNumberRows()
    {
        return m_rows.size();
    }

    public String getProperty(String key, String def)
    {
        return m_properties.getProperty(key, def);
    }

    public void read(File file) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        m_lineNumber = 0;
        m_propertiesRead = false;
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            ++m_lineNumber;
            handleLine(line);
        }
        reader.close();
    }

    public void save(Writer out) throws IOException
    {
        Enumeration propertyNames = m_properties.propertyNames();
        for (Enumeration e = propertyNames; e.hasMoreElements(); )
        {
            String key = (String)e.nextElement();
            out.write("# " + key + ": " + m_properties.get(key) + "\n");
        }
        out.write("#\n#");
        for (int i = 0; i < m_numberColumns; ++i)
        {
            out.write(getColumnTitle(i));
            if (i < m_numberColumns - 1)
                out.write('\t');
            else
                out.write('\n');
        }
        for (int i = 0; i < m_rows.size(); ++i)
        {
            Vector row = (Vector)m_rows.get(i);
            for (int j = 0; j < m_numberColumns; ++j)
            {
                String value = (String)row.get(j);
                if (value == null)
                    out.write("(null)");
                else
                    out.write(value);
                if (j < m_numberColumns - 1)
                    out.write('\t');
                else
                    out.write('\n');
            }
        }
    }
    
    public void set(int column, String value)
    {
        assert(m_lastRow.get(column) == null);
        m_lastRow.set(column, value);
    }

    public void set(String column, int value)
    {
        set(column, Integer.toString(value));
    }

    public void set(String column, double value)
    {
        set(column, Double.toString(value));
    }

    public void set(String column, String value)
    {
        set(getColumnIndex(column), value);
    }

    public Object setProperty(String key, String value)
    {
        return m_properties.setProperty(key, value);
    }

    public void startRow()
    {
        Vector row = new Vector(m_numberColumns);
        for (int i = 0; i < m_numberColumns; ++i)
            row.add(null);
        m_rows.add(row);
        m_lastRow = row;
    }
    
    private boolean m_propertiesRead;

    private int m_lineNumber;

    private int m_numberColumns;

    private Properties m_properties = new Properties();

    private Vector m_columnTitles;

    private Vector m_lastRow;

    private Vector m_rows = new Vector();

    private void addColumnTitle(String columnTitle)
    {
        m_columnTitles.add(columnTitle);
        ++m_numberColumns;
    }

    private Vector getRow(int index)
    {
        return (Vector)m_rows.get(index);
    }

    private static String getCommentValue(String comment, String key)
    {
        assert(comment.startsWith(key));
        return comment.substring(key.length()).trim();
    }

    private void handleComment(String comment)
    {
        comment = comment.trim();
        if (m_propertiesRead)
        {
            String[] array = comment.split("\\t");
            for (int i = 0; i < array.length; ++i)
                addColumnTitle(array[i]);
            return;
        }
        if (comment.equals(""))
        {
            m_propertiesRead = true;
            return;
        }
        int pos = comment.indexOf(':');
        if (pos < 0)
        {
            System.err.println("Invalid line: " + comment);
            return;
        }
        String key = comment.substring(0, pos).trim();
        String value = comment.substring(pos + 1).trim();
        setProperty(key, value);
    }

    private void handleLine(String line) throws ErrorMessage
    {
        line = line.trim();
        if (line.startsWith("#"))
        {
            handleComment(line.substring(1));
            return;
        }
        String[] array = line.split("\\t");
        startRow();
        for (int i = 0; i < array.length; ++i)
            set(i, array[i]);
    }
}

//----------------------------------------------------------------------------
