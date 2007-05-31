//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.game;

import net.sf.gogui.util.ErrorMessage;

public final class TimeSettingsTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(TimeSettingsTest.class);
    }

    public void testParse()
    {
        TimeSettings s;

        s = parse("30");
        assertEquals(30 * 60 * 1000, s.getPreByoyomi());
        assertFalse(s.getUseByoyomi());

        s = parse("30+20/10");
        assertEquals(30 * 60 * 1000, s.getPreByoyomi());
        assertTrue(s.getUseByoyomi());
        assertEquals(20 * 60 * 1000, s.getByoyomi());
        assertEquals(10, s.getByoyomiMoves());
    }

    private TimeSettings parse(String s)
    {
        try
        {
            return TimeSettings.parse(s);
        }
        catch (ErrorMessage e)
        {
            fail();
            return null;
        }
    }
}
