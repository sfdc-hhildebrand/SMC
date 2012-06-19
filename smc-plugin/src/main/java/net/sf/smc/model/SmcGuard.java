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
// RCS ID
// $Id: SmcGuard.java,v 1.3 2009/10/06 15:31:59 kgreg99 Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the second half of a
 * {@link net.sf.smc.model.SmcTransition transition} definition.
 * This contains the:
 * <ul>
 *   <li>
 *     reference to the parent transition,
 *   </li>
 *   <li>
 *     the guard condition (which may be an empty string),
 *   </li>
 *   <li>
 *     the transition type,
 *   </li>
 *   <li>
 *     the end state,
 *   </li>
 *   <li>
 *     the push state (if this is a push transition type),
 *   </li>
 *   <li>
 *     the pop arguments (if this is a pop transition type) and
 *   </li>
 *   <li>
 *     the transition action list (may be empty).
 *   </li>
 * </ul>
 * Most transitions have no condition, go directly to the next
 * state and have zero or more actions. The push and pop
 * transitions allow FSM "subroutines" to be defined. If a common
 * state machine sequence needs to be accessed from different
 * states in the main map, then SMC encourages developers to
 * define that common sequence in a separate map and to push
 * to this submap from the main map states. When the submap
 * completes its work a pop transition is used to return to
 * the point of departure in the main map. The pop transition
 * "returns" a transition and optional argument list which is
 * applied to the restored main map state. In short, push and
 * pop transitions allow redundant state sequences to be defined
 * once instead of duplicated multiple times within a single FSM.
 *
 * @see net.sf.smc.model.SmcTransition
 * @see net.sf.smc.model.SmcAction
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcGuard
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a guard instance for the given transition, line
     * number and condition.
     * @param cond transition guard condition.
     * @param lineNumber where this guard appears in the .sm
     * file.
     * @param transition guard belongs to this transition.
     */
    public SmcGuard(String cond,
                    int lineNumber,
                    SmcTransition transition)
    {
        super (transition.getName(), lineNumber);

        _transition = transition;
        _condition = cond;
        _endState = "";
        _pushState = "";
        _actions = null;
        _popArgs = "";
    } // end of SmcGuard(String, int, SmcTransition)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcElement Abstract Methods.
    //

    /**
     * Calls the visitor's visit method for this finite state
     * machine element.
     * @param visitor The visitor instance.
     * @see SmcVisitor
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
    // Get methods.
    //

    /**
     * Returns the transition to which this guard belongs.
     * @return the transition to which this guard belongs.
     */
    public SmcTransition getTransition()
    {
        return (_transition);
    } // end getTransition()

    /**
     * Returns the guard condition.
     * @return the guard condition.
     */
    public String getCondition()
    {
        return(_condition);
    } // end of getCondition()

    /**
     * Returns the transition type.
     * @return the transition type.
     */
    public TransType getTransType()
    {
        return(_transType);
    } // end of getTransType()

    /**
     * Returns the transition end state name.
     * @return the transition end state name.
     */
    public String getEndState()
    {
        return(_endState);
    } // end of getEndState()

    /**
     * Returns the push state name. This is only valid if the
     * transition is a
     * {@link net.sf.smc.model.SmcElement.TransType#TRANS_PUSH push}
     * transition. This state become the current state after the
     * transition completes. It is <i>not</i> the state pushed on
     * to the state stack.
     * @return the push state name.
     */
    public String getPushState()
    {
        return (_pushState);
    } // end of getPushState()

    /**
     * Returns the pop transition arguments. This is only valid
     * if the transition type is a
     * {@link net.sf.smc.model.SmcElement.TransType#TRANS_POP pop}
     * transition.
     * @return the pop transition arguments.
     */
    public String getPopArgs()
    {
        return (_popArgs);
    } // end of getPopArgs()

    /**
     * Returns {@code true} if this guard references the
     * {@code ctxt} variable and {@code false} otherwise. The
     * {@code ctxt} variable plays a similar rote as {@code this}
     * or {@code self} in object-oriented programming languages.
     * {@code ctxt} is the first parameter for all transitions
     * and is a reference to the FSM context instance (even if
     * a transitions has no defined parameters it still has this
     * one).
     * @return {@code true} if this guard references the
     * {@code ctxt} variable and {@code false} otherwise.
     */
    public boolean hasCtxtReference()
    {
        boolean retcode = false;

        // The ctxt variable may appear in the condition, the
        // actions or in the pop arguments.
        if ((_condition != null &&
             (_condition.indexOf("ctxt ") >= 0 ||
              _condition.indexOf("ctxt.") >= 0 ||
              _condition.indexOf("ctxt->") >= 0 ||
              _condition.indexOf("ctxt:") >= 0)) ||
            _hasActions() == true ||
            (_transType == TransType.TRANS_POP &&
             _popArgs != null &&
             (_popArgs.indexOf("ctxt ") >= 0 ||
              _popArgs.indexOf("ctxt.") >= 0 ||
              _popArgs.indexOf("ctxt->") >= 0 ||
              _popArgs.indexOf("ctxt:") >= 0)))
        {
            retcode = true;
        }

        return (retcode);
    } // end of hasCtxtReference()

    /**
     * Returns the transition action list.
     * @return the transition action list.
     */
    public List<SmcAction> getActions()
    {
        return (_actions);
    } // end of getActions()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the transition type.
     * @param transType the transition type.
     */
    public void setTransType(TransType transType)
    {
        _transType = transType;
        return;
    } // end of setTransType(TransType)

    /**
     * Sets the transition end state name.
     * @param endState the end state name.
     */
    public void setEndState(String endState)
    {
        _endState = endState;
        return;
    } // end of setEndState(String)

    /**
     * Sets the push state name.
     * @param state a state name.
     */
    public void setPushState(String state)
    {
        _pushState = state;
        return;
    } // end of setPushState(String)

    /**
     * Set the pop transition arguments.
     * @param args pop transition arguments.
     */
    public void setPopArgs(String args)
    {
        _popArgs = args;
        return;
    } // end of setPopArgs(String)

    /**
     * Sets the transition actions. May not be {@code null} but
     * may be empty.
     * @param actions transition actions.
     */
    public void setActions(List<SmcAction> actions)
    {
        _actions = new ArrayList<SmcAction>(actions);
        return;
    } // end of setActions(List<SmcAction>)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Returns the transition guard text representation.
     * @return the transition guard text representation.
     */
    @Override
    public String toString()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.print(_name);

        if (_condition.length() > 0)
        {
            pw.print(" [");
            pw.print(_condition);
            pw.print("]");
        }

        switch(_transType)
        {
            case TRANS_NOT_SET:
                pw.print(" not set");
                break;

            case TRANS_SET:
            case TRANS_PUSH:
                pw.print(" set");
                break;

            case TRANS_POP:
                pw.print(" pop");
                break;
        }

        pw.print(" ");
        pw.print(_endState);

        if (_transType == TransType.TRANS_PUSH)
        {
            pw.print("/");
            pw.print(" push(");
            pw.print(_pushState);
            pw.print(")");
        }

        pw.println(" {");
        if (_actions.isEmpty() == false)
        {
            for (SmcAction action: _actions)
            {
                pw.print("    ");
                pw.print(action);
                pw.println(";");
            }
        }
        pw.print("}");

        return(sw.toString());
    } // end of toString()

    // Scope the state name. If the state is unscoped, then
    // return "<mapName>.<stateName>". If the state named
    // contains the scope string "::", replace that with a ".".
    private String _scopeStateName(final String stateName,
                                   final String mapName)
    {
        int index;
        StringWriter retval = new StringWriter();

        index = stateName.indexOf("::");
        if (index < 0)
        {
            retval.write(mapName);
            retval.write(".");
            retval.write(stateName);
        }
        else
        {
            retval.write(stateName.substring(0, index));
            retval.write('.');
            retval.write(stateName.substring(index + 2));
        }

        return (retval.toString());
    }

    // The ctxt local variable is created if:
    // 1. The action list is not null.
    // 2. The action list has 1 or more items of which
    //    one is *not* emptyStateStack.
    private boolean _hasActions()
    {
        boolean retcode = false;

        if (_actions != null && _actions.isEmpty() == false)
        {
            Iterator<SmcAction> ait;
            SmcAction action;

            for (ait = _actions.iterator();
                 ait.hasNext() == true && retcode == false;
                )
            {
                action = ait.next();
                retcode = !action.isEmptyStateStack();
            }
        }

        return (retcode);
    } // end of hasActions(List<SmcAction>)

//---------------------------------------------------------------
// Member data.
//

    private SmcTransition _transition;
    private String _condition;
    private TransType _transType;
    private String _endState;
    private String _pushState;
    private String _popArgs;
    private List<SmcAction> _actions;

} // end of class SmcGuard

//
// CHANGE LOG
// $Log: SmcGuard.java,v $
// Revision 1.3  2009/10/06 15:31:59  kgreg99
// 1. Started implementation of feature request #2718920.
//     1.1 Added method boolean isStatic() to SmcAction class. It returns false now, but is handled in following language generators: C#, C++, java, php, VB. Instance identificator is not added in case it is set to true.
// 2. Resolved confusion in "emtyStateStack" keyword handling. This keyword was not handled in the same way in all the generators. I added method boolean isEmptyStateStack() to SmcAction class. This method is used instead of different string comparisons here and there. Also the generated method name is fixed, not to depend on name supplied in the input sm file.
//
// Revision 1.2  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.13  2008/02/08 08:41:58  fperrad
// fix Bug 1883822 : Lua - missing ctxt
//
// Revision 1.12  2007/12/28 12:34:41  cwrapp
// Version 5.0.1 check-in.
//
// Revision 1.11  2007/02/21 13:54:54  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.10  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.9  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.8  2006/06/03 19:39:25  cwrapp
// Final v. 4.3.1 check in.
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
// Revision 1.7  2005/02/21 15:35:24  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.6  2005/02/21 15:14:07  charlesr
// Moved isLoopback() method from this class to SmcCodeGenerator.
//
// Revision 1.5  2005/02/03 16:45:49  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.4  2004/11/14 18:24:59  charlesr
// Minor improvements to the DOT file output.
//
// Revision 1.3  2004/10/30 16:04:39  charlesr
// Added Graphviz DOT file generation.
// Added getPopArgs() method.
//
// Revision 1.2  2004/09/06 16:40:01  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:54:03  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:03:28  charlesr
// Initial revision
//
