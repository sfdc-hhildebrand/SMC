//
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy
// of the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// The Original Code is State Machine Compiler (SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2005, 2008. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s):
//   Eitan Suez contributed examples/Ant.
//   (Name withheld) contributed the C# code generation and
//   examples/C#.
//   Francois Perrad contributed the Python code generation and
//   examples/Python.
//   Chris Liscio contributed the Objective-C code generation
//   and examples/ObjC.
//
// RCS ID
// $Id: SmcMessage.java,v 1.1 2009/03/01 18:20:42 cwrapp Exp $
//
// CHANGE LOG
// (See bottom of file.)
//

package net.sf.smc.parser;

/**
 * Stores a warning or error message, the .sm file line number on
 * which it occurred and the FSM's name. The parser and syntax
 * checker do not output errors directly. Instead they store
 * errors in a list and the calling application decides how to
 * present the messages.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcMessage
{
//---------------------------------------------------------------
// Member methods
//

    /**
     * Creates a message for the given FSM, line number, level
     * and text.
     * @param name the finite state machine's name.
     * @param lineNumber the error's line number.
     * @param level the message level. Must be either
     * {@link #WARNING} or {@link #ERROR}.
     * @param text the message text.
     * @exception NullPointerException
     * if either {@code name} or {@code text} is
     * {@code null}.
     * @exception IllegalArgumentException
     * if:
     * <ul>
     *   <li>
     *     {@code name} is an empty string.
     *   </li>
     *   <li>
     *     {@code lineNumber} is &lt; zero.
     *   </li>
     *   <li>
     *     {@code level} is neither {@link #WARNING} nor
     *     {@link #ERROR}.
     *   </li>
     *   <li>
     *     {@code text} is an empty string.
     *   </li>
     * </ul>
     */
    public SmcMessage(String name,
                      int lineNumber,
                      int level,
                      String text)
    {
        if (name == null)
        {
            throw (new NullPointerException("null name"));
        }
        else if (text == null)
        {
            throw (new NullPointerException("null text"));
        }
        else if (name.length() == 0)
        {
            throw (new IllegalArgumentException("empty name"));
        }
        else if (lineNumber < 0)
        {
            throw (
                new IllegalArgumentException(
                    "negative lineNumber"));
        }
        else if (level != WARNING && level != ERROR)
        {
            throw (
                new IllegalArgumentException(
                    "invalid level (" +
                    Integer.toString(level) +
                    ")"));
        }
        else if (text.length() == 0)
        {
            throw (new IllegalArgumentException("empty text"));
        }
        else
        {
            _name = name;
            _lineNumber = lineNumber;
            _level = level;
            _text = text;
        }
    }

    /**
     * Returns the finite state machine's name.
     * @return the finite state machine's name.
     */
    public String getName()
    {
        return (_name);
    }

    /**
     * Returns the line number.
     * @return the line number.
     */
    public int getLineNumber()
    {
        return (_lineNumber);
    }

    /**
     * Returns the message level, either {@link #WARNING} or
     * {@link #ERROR}.
     * @return the message level.
     */
    public int getLevel()
    {
        return (_level);
    }

    /**
     * Returns the message text.
     * @return the message text.
     */
    public String getText()
    {
        return (_text);
    }

//---------------------------------------------------------------
// Member data
//

    private final String _name;
    private final int _lineNumber;
    private final int _level;
    private final String _text;

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * A warning-level message (0).
     */
    public static final int WARNING = 0;

    /**
     * An error-level message (1).
     */
    public static final int ERROR = 1;
}

// CHANGE LOG
// $Log: SmcMessage.java,v $
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.3  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.2  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.2  2005/02/21 15:37:16  charlesr
// Corrected header comment.
//
// Revision 1.1  2005/02/21 15:36:35  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.0  2005/02/03 17:11:47  charlesr
// Initial revision
//
