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
// $Id: SmcElement.java,v 1.1 2009/03/01 18:20:42 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The super class for all abstract syntax tree elements.
 * Provides the interface for the visitor classes.
 * See the Visitor pattern in GoF (p. 331).
 * @see SmcFSM
 * @see SmcMap
 * @see SmcState
 * @see SmcTransition
 * @see SmcGuard
 * @see SmcAction
 * @see SmcParameter
 * @see SmcVisitor
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Default constructor.
     * @param name the elements unique name.
     * @param lineNumber where this element appears in the .sm
     * file.
     */
    protected SmcElement(String name, int lineNumber)
    {
        _name = name;
        _lineNumber = lineNumber;
    } // end of SmcElement(String, int)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Abstract methods.
    //

    /**
     * Accepts a new visitor which performs some action upon this
     * abstract syntax tree element.
     * @param visitor a parser visitor.
     */
    public abstract void accept(SmcVisitor visitor);

    //
    // end of Abstract methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the element's unique name.
     * @return the element's unique name.
     */
    public String getName()
    {
        return (_name);
    } // end of getName()

    /**
     * Returns the element's position in the .sm file.
     * @return the element's position in the .sm file.
     */
    public int getLineNumber()
    {
        return(_lineNumber);
    } // end of getLineNumber()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    /**
     * Returns an ordered transition list with no repeated
     * entries by merging two transition lists together.
     * @param l1 The first transition list.
     * @param l2 The second transition list.
     * @param c Transition comparator.
     * @return an ordered transition list with no repeated
     * entries by merging two transition lists together.
     */
    public static List<SmcTransition>
        merge(List<SmcTransition> l1,
              List<SmcTransition> l2,
              Comparator<SmcTransition> c)
    {
        int result;
        Iterator<SmcTransition> it1;
        Iterator<SmcTransition> it2;
        SmcTransition e1;
        SmcTransition e2;
        List<SmcTransition> retval =
            new ArrayList<SmcTransition>();

        // First, make certain that both lists are sorted.
        Collections.sort(l1, c);
        Collections.sort(l2, c);

        // Now merge the two lists together.
        // Continue until the end of either list is reached.
        for (it1 = l1.iterator(),
                     it2 = l2.iterator(),
                     e1 = null,
                     e2 = null;
             (it1.hasNext() == true || e1 != null) &&
                     (it2.hasNext() == true || e2 != null);
            )
        {
            if (e1 == null)
            {
                e1 = it1.next();
            }
            if (e2 == null)
            {
                e2 = it2.next();
            }

            if ((result = c.compare(e1, e2)) < 0)
            {
                retval.add(e1);
                e1 = null;
            }
            else if (result > 0)
            {
                retval.add(e2);
                e2 = null;
            }
            else
            {
                retval.add(e1);
                e1 = null;
                e2 = null;
            }
        }

        // Is there any more to add?
        if (it1.hasNext() == true || e1 != null)
        {
            if (e1 != null)
            {
                retval.add(e1);
            }

            for (; it1.hasNext() == true;)
            {
                retval.add(it1.next());
            }
        }
        else if (it2.hasNext() == true || e2 != null)
        {
            if (e2 != null)
            {
                retval.add(e2);
            }

            for (; it2.hasNext() == true;)
            {
                retval.add(it2.next());
            }
        }

        return (retval);
    } // end of merge(List<>, List<>, Comparator)

//---------------------------------------------------------------
// Member data
//

    /**
     * An element has a unique name.
     */
    protected final String _name;

    /**
     * The line number where this element is defined in the .sm
     * file.
     */
    protected final int _lineNumber;

    //-----------------------------------------------------------
    // Constants.
    //

    /**
     * Inner loopback transitions use "nil" as their destination
     * state.
     */
    public static final String NIL_STATE = "nil";

    /**
     * The SMC transitions fall into four types.
     */
    public enum TransType
    {
        /**
         * Transitions may be instantiated before all information
         * is gathered. This is the default transition type.
         */
        TRANS_NOT_SET,

        /**
         * A standard transition which goes from one state to the
         * next.
         */
        TRANS_SET,

        /**
         * This transition pushes the current state on top of the
         * state stack before setting the next state. The previous
         * state may be restored using a pop transition. A push
         * transtion may be used in conjunction with a set
         * transition. In that case the specified next state is
         * pushed on top of the state stack and not the current
         * state.
         */
        TRANS_PUSH,

        /**
         * Pop the state off the state stack and make it the next
         * state.
         */
        TRANS_POP
    } // end of enum TransType
} // end of class SmcElement

//
// CHANGE LOG
// $Log: SmcElement.java,v $
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
// Revision 1.1  2005/02/21 15:35:05  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.0  2005/02/03 17:08:39  charlesr
// Initial revision
//
