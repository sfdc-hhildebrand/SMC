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
// Copyright (C) 2000 - 2008. Charles W. Rapp.
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
// SmcAction --
//
//  Stores a state machine action. May be associated with a
//  transition, a state's entry or exit.
//
// RCS ID
// $Id: SmcAction.java,v 1.2 2009/10/06 15:31:59 kgreg99 Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Actions are used in both transtions and state Entry and Exit
 * clauses. Actions are implemented as methods in the FSM's
 * context class.
 * <p>
 * Actions have two properties:
 * <ol>
 *   <li>
 *     Property: if this flag is {@code true}, then the action
 *     was a property assignment (var = value). This action is
 *     supported only for C# and VB.Net target compilation.
 *   </li>
 *   <li>
 *     Arguments: the action's arguments copied verbatim from
 *     the SMC code. The SMC parser does not attempt to
 *     interpret the actions beyond requiring that the arguments
 *     are a comma-separated list.
 *   </li>
 * </ol>
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcAction
    extends SmcElement
    implements Comparable<SmcAction>
{
//---------------------------------------------------------------
// Member Methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates the named action appearing on the given .sm line.
     * The action has no arguments and is not a property
     * assignment by default.
     * @param name the action's name should correspond to the
     * FSM context method.
     * @param lineNumber where the action appears in the .sm
     * file.
     */
    public SmcAction(String name, int lineNumber)
    {
        super (name, lineNumber);

        _arguments = null;
        _propertyFlag = false;
        _staticFlag = false;
    } // end of SmcAction(String, int)

    /**
     * Creates an action with all data members specified.
     * @param name the action's name should correspond to the
     * FSM context method.
     * @param lineNumber where the action appears in the .sm
     * file.
     * @param propertyFlag if {@code true}, then this action is
     * a .Net property assignment and {@code arguments} must be
     * a non-{@code null} list with exactly one item.
     * @param arguments the action's arguments. May be
     * {@code null}.
     * @exception IllegalArgumentException
     * if {@code propertyFlag} is {@code true} and
     * {@code arguments} is either {@code null} or does not
     * contain exactly one item.
     */
    public SmcAction(String name,
                     int lineNumber,
                     boolean propertyFlag,
                     List<String> arguments)
        throws IllegalArgumentException
    {
        super (name, lineNumber);

        if (propertyFlag == true &&
            (arguments == null ||
             arguments.size() != 1))
        {
            throw (
                new IllegalArgumentException(
                    "property must have exactly one argument"));
        }
        else
        {
            _arguments = arguments;
            _propertyFlag = propertyFlag;
        }
    } // end of SmcAction(String, boolean, List<String>, int)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    /**
     * Pass this action to the visitor for processing. Part of
     * the Visitor pattern.
     * @param visitor the object implementing the
     * visit(SmcElement) method.
     */
    @Override
    public void accept(SmcVisitor visitor)
    {
        visitor.visit(this);
    } // end of accept(SmcVisitor)

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Comparable Interface Implementation.
    //

    /**
     * Returns an integer value &lt;, = or &gt; zero depending
     * on whether this action is &lt;, = or &gt; {@code action}.
     * The comparison is based on the action name and arguments.
     * @param action comparison action object.
     * @return an integer value &lt;, = or &gt; zero depending
     * on whether this action is &lt;, = or &gt; {@code action}.
     */
    @Override
    public int compareTo(SmcAction action)
    {
        int retval = 0;

        if (this != action &&
            (retval = _name.compareTo(action.getName())) == 0)
        {
            Iterator<String> ait1;
            Iterator<String> ait2;
            String s1;
            String s2;

            for (ait1 = _arguments.iterator(),
                     ait2 = action._arguments.iterator();
                 ait1.hasNext() == true &&
                     ait2.hasNext() == true &&
                     retval == 0;
                )
            {
                s1 = ait1.next();
                s2 = ait2.next();

                retval = s1.compareTo(s2);
            }
        }

        return (retval);
    } // end of compareTo(SmcAction)

    //
    // end of Comparable Interface Implementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns {@code true} if this action is a .Net property
     * assignment and {@code false} if not.
     * @return {@code true} if this action is a .Net property
     * assignment and {@code false} if not.
     */
    public boolean isProperty()
    {
        return (_propertyFlag);
    } // end of isProperty()
    
    /**
    * Returns {@code true} if this action is a static member.
    * Returns {@code false} if this action is an instance member.
    */
    public boolean isStatic()
    {
    	return (_staticFlag);
    }// end of isStatic
    
    /**
    * Returns {@code true} if this action is the predefined EmptyStateStack()
    * Returns {@code false} if this action is not the predefined EmptyStateStack()
    */
    public boolean isEmptyStateStack()
    {
    	return EMPTY_STATE_STACK.equalsIgnoreCase( this.getName() );
    } // end of isEmptyStateStack
    
    /**
     * Returns the action's argument list. May return
     * {@code null}.
     * @return the action's argument list. May return
     * {@code null}.
     */
    public List<String> getArguments()
    {
        return (_arguments);
    } // end of getArguments()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * If {@code flag} is {@code true}, then this action is a
     * .Net property assignment.
     * @param flag if {@code true}, then this action is a .Net
     * property assignment.
     */
    public void setProperty(boolean flag)
    {
        _propertyFlag = flag;
        return;
    } // end of setProperty(boolean)

    /**
     * Sets the action's arguments.
     * @param args the action's arguments. May be {@code null}.
     * @exception IllegalArgumentException
     * if this is a property action and {@code arguments} is
     * either {@code null} or does not contain exactly one item.
     */
    public void setArguments(List<String> args)
        throws IllegalArgumentException
    {
        if (_propertyFlag == true &&
            (args == null || args.size() != 1))
        {
            throw (
                new IllegalArgumentException(
                    "property must have exactly one argument"));
        }
        else
        {
            _arguments = new ArrayList<String>(args);
        }

        return;
    } // end of setArguments(List<String>)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Returns a textual representation of this action. If this
     * is a .Net property, the format is
     * "&lt;name&gt; = &lt;argument&gt;". Otherwise the format is
     * "&lt;name&gt;(&lt;arg0&gt;[, &lt;argn&gt;])".
     * @return a textual representation of this action.
     */
    @Override
    public String toString()
    {
        StringBuffer retval = new StringBuffer(40);

        retval.append(_name);

        if (_propertyFlag == true)
        {
            retval.append(" = ");
            retval.append(_arguments.get(0));
        }
        else
        {
            Iterator<String> ait;
            String sep;

            retval.append('(');

            for (ait = _arguments.iterator(), sep = "";
                 ait.hasNext() == true;
                 sep = ", ")
            {
                retval.append(sep);
                retval.append(ait.next());
            }

            retval.append(')');
        }

        return (retval.toString());
    } // end of toString()

//---------------------------------------------------------------
// Member Data
//

    // The action's argument list.
    private List<String> _arguments;

    // Is this action a .Net property assignment?
    private boolean _propertyFlag;
    
    // Is this action a static member ?
    private boolean _staticFlag;

    //-----------------------------------------------------------
    // Constants.
    //
    private static final String EMPTY_STATE_STACK =
        "emptystatestack";
    
} // end of class SmcAction

//
// CHANGE LOG
// $Log: SmcAction.java,v $
// Revision 1.2  2009/10/06 15:31:59  kgreg99
// 1. Started implementation of feature request #2718920.
//     1.1 Added method boolean isStatic() to SmcAction class. It returns false now, but is handled in following language generators: C#, C++, java, php, VB. Instance identificator is not added in case it is set to true.
// 2. Resolved confusion in "emtyStateStack" keyword handling. This keyword was not handled in the same way in all the generators. I added method boolean isEmptyStateStack() to SmcAction class. This method is used instead of different string comparisons here and there. Also the generated method name is fixed, not to depend on name supplied in the input sm file.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.11  2007/08/05 14:36:11  cwrapp
// Version 5.0.1 check-in. See net/sf/smc/CODE_README.txt for more informaiton.
//
// Revision 1.10  2007/02/21 13:53:52  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.9  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.8  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.7  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of transition
//   names. This feature is useful to GUI developers who want to
//   enable/disable features based on the current state. See
//   Programmer's Manual section 11: On Reflection for more
//   information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools directory.
//   Added Eiten Suez's SMC tutorial (in PDF) to a new docs
//   directory.
//
// Fixed the following bugs:
//
// + (GraphViz) DOT file generation did not properly escape
//   double quotes appearing in transition guards. This has been
//   corrected.
//
// + A note: the SMC FAQ incorrectly stated that C/C++ generated
//   code is thread safe. This is wrong. C/C++ generated is
//   certainly *not* thread safe. Multi-threaded C/C++ applications
//   are required to synchronize access to the FSM to allow for
//   correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.6  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:34:32  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/03 16:43:05  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator).
//
// Revision 1.3  2004/10/30 16:04:01  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.2  2004/09/06 16:39:31  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:53:34  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:03:05  charlesr
// Initial revision
//
