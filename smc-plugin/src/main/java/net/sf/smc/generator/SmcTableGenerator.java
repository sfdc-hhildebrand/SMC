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
// $Id: SmcTableGenerator.java,v 1.6 2010/03/05 21:29:53 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.util.Iterator;
import java.util.List;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;
import net.sf.smc.model.SmcVisitor;

/**
 * Visits the abstract syntax tree, emitting an HTML table.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcTableGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a HTML table code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcTableGenerator(final SmcOptions options)
    {
        super (options, "html");
    } // end of SmcTableGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits HTML table code for the finite state machine.
     * @param fsm emit HTML table code for this finite state machine.
     */
    public void visit(SmcFSM fsm)
    {
        Iterator<SmcMap> mit;
        String separator;

        // Output the top-of-page HTML.
        _source.println("<html>");
        _source.println("  <head>");
        _source.print("    <title>");
        _source.print(_srcfileBase);
        _source.println("</title>");
        _source.println("  </head>");
        _source.println();
        _source.println("  <body>");

        // Have each map generate its HTML table.
        for (mit = fsm.getMaps().iterator(), separator = "";
             mit.hasNext();
             separator = "    <p>\n")
        {
            _source.print(separator);
            (mit.next()).accept(this);
        }

        // Output the end-of-page HTML.
        _source.println("  </body>");
        _source.println("</html>");

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits HTML table code for the FSM map.
     * @param map emit HTML table code for this map.
     */
    public void visit(SmcMap map)
    {
        String mapName = map.getName();
        List<SmcTransition> transitions = map.getTransitions();
        List<SmcParameter> params;
        int transitionCount = transitions.size() + 1;
        Iterator<SmcParameter> it;
        SmcTransition defaultTransition = null;
        String transName;
        boolean firstFlag;

        // Output start of this map's table.
        _source.println(
            "    <table align=center border=3 cellspacing=2 cellpadding=2>");
        _source.println("      <caption align=\"top\">");
        _source.print("        ");
        _source.print(mapName);
        _source.println(" Finite State Machine");
        _source.println("      </caption>");

        // Output the table's header.
        _source.println("      <tr>");
        _source.println("        <th rowspan=2>");
        _source.println("          State");
        _source.println("        </th>");
        _source.println("        <th colspan=2>");
        _source.println("          Actions");
        _source.println("        </th>");
        _source.print("        <th colspan=");
        _source.print(transitionCount);
        _source.println(">");
        _source.println("          Transition");
        _source.println("        </th>");
        _source.println("      </tr>");
        _source.println("      <tr>");
        _source.println("        <th>");
        _source.println("          Entry");
        _source.println("        </th>");
        _source.println("        <th>");
        _source.println("         Exit");
        _source.println("        </th>");

        // Place each transition name into the header.
        for (SmcTransition transition: transitions)
        {
            transName = transition.getName();
            params = transition.getParameters();

            // Since we are placing the default transition at the
            // right-most column, don't output it here if it
            // should locally defined.
            if (transName.equals("Default") == false)
            {
                _source.println("        <th>");
                _source.print("          ");
                _source.println(transName);

                // If the transition has parameters, output
                // them now.
                if (params.isEmpty() == false)
                {
                    _source.println("          <br>");
                    _source.print("          (");

                    for (it = params.iterator(),
                             firstFlag = true;
                         it.hasNext() == true;
                         firstFlag = false)
                    {
                        if (firstFlag == false)
                        {
                            _source.println(',');
                            _source.println("          <br>");
                            _source.print("          ");
                        }

                        (it.next()).accept(this);
                    }

                    _source.println(")");
                }

                _source.println("        </th>");
            }
        }

        // Also output the default transition.
        _source.println("        <th>");
        _source.println("          <b>Default</b>");
        _source.println("        </th>");
        _source.println("      </tr>");

        // The table header is finished. Now have each state
        // output its row.
        for (SmcState state: map.getStates())
        {
            // Output the row start.
            _source.println("      <tr>");

            // Note: the state outputs only its name and
            // entry/exit actions. It does not output its
            // transitions (see below).
            state.accept(this);

            // We need to generate transitions in the exact same
            // order as in the header. But the state object
            // does not store its transitions in any particular
            // order. Therefore we must output the state's
            // transitions for it.
            for (SmcTransition transition: transitions)
            {
                transName = transition.getName();
                params = transition.getParameters();

                // Since we are placing the default transition
                // at the right-most column, don't output it
                // here if it should locally defined.
                if (transName.equals("Default") == true)
                {
                    // If this state has a false transition,
                    // get it now and store it away for later.
                    defaultTransition =
                        state.findTransition(transName, params);
                }
                else
                {
                    _source.println("        <td>");

                    // We have the default transition definition
                    // in hand. We need the state's transition.
                    transition =
                        state.findTransition(transName, params);
                    if (transition != null)
                    {
                        // Place the transitions in preformatted
                        // sections. Don't add a new line - the
                        // transition will do that.
                        _source.print("          <pre>");
                        transition.accept(this);
                        _source.println("          </pre>");
                    }

                    _source.println("        </td>");
                }
            }

            // Now add the Default transition to the last column.
            _source.println("        <td>");
            if (defaultTransition != null)
            {
                // Place the transitions in preformatted
                // sections. Don't add a new line - the
                // transition will do that.
                _source.print("          <pre>");
                defaultTransition.accept(this);
                _source.println("          </pre>");
            }
            _source.println("        </td>");

            // Output the row end.
            _source.println("      </tr>");
        }

        // Output end of this map's table.
        _source.println("    </table>");

        return;
    } // end of visit(SmcMap)

    /**
     * Emits HTML table code for this FSM state.
     * @param state emits HTML table code for this state.
     */
    public void visit(SmcState state)
    {
        List<SmcAction> actions;

        // Output the row data. This consists of:
        // + the state name.
        // + the state entry actions.
        // + the state exit actions.
        // + Each of the transtions.
        _source.println("        <td>");
        _source.print("          ");
        _source.println(state.getInstanceName());
        _source.println("        </td>");

        _source.println("        <td>");
        actions = state.getEntryActions();
        if (actions != null && actions.isEmpty() == false)
        {
            _source.println("          <pre>");

            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _source.println("          </pre>");
        }
        _source.println("        </td>");

        _source.println("        <td>");
        actions = state.getExitActions();
        if (actions != null && actions.isEmpty() == false)
        {
            _source.println("          <pre>");
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _source.println("          </pre>");
        }
        _source.println("        </td>");

        // Note: SmcMap generates our transitions for us in order
        //       to guarantee correct transition ordering.

        return;
    } // end of visit(SmcState)

    /**
     * Emits HTML table code for this FSM state transition.
     * @param transition emits HTML table code for this state
     * transition.
     */
    public void visit(SmcTransition transition)
    {
        for (SmcGuard guard: transition.getGuards())
        {
            _source.println();
            guard.accept(this);
        }

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits HTML table code for this FSM transition guard.
     * @param guard emits HTML table code for this transition
     * guard.
     */
    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String mapName = map.getName();
        String stateName = state.getClassName();
        TransType transType = guard.getTransType();
        String condition = guard.getCondition();
        String endStateName = guard.getEndState();
        List<SmcAction> actions = guard.getActions();

        // Print out the guard (if there is one).
        if (condition.length() > 0)
        {
            _source.print('[');
            _source.print(condition);
            _source.println(']');
        }

        // If this is a pop transition, then print
        // out the pop transition and any arguments.
        if (transType == TransType.TRANS_POP)
        {
            _source.print("  pop(");

            // Is there a pop transition?
            if (endStateName.equals(
                    SmcElement.NIL_STATE) == false &&
                endStateName.length() > 0)
            {
                String popArgs = guard.getPopArgs();

                _source.print(endStateName);

                // Output any and all pop arguments.
                if (popArgs.length() > 0)
                {
                    _source.print(", ");
                    _source.print(popArgs.trim());
                }
            }

            _source.println(")");
        }
        else if (transType == TransType.TRANS_PUSH)
        {

            // If the end state is nil, then replace it with the
            // current map and state.
            if (!endStateName.equals(
                    SmcElement.NIL_STATE))
            {
                _source.print("  ");
                _source.print(endStateName);
                _source.print("/push(");
            } else { 
                _source.print("  push(");
            }
                _source.print(mapName);
                _source.print("::");
                _source.print(stateName);

            _source.println(")");
        }
        // Else this is a plain, old transition.
        else
        {
            // Print out the end state.
            _source.print("  ");

            // If the end state is nil, then replace it with the
            // current state's read name.
            if (endStateName.equals(
                    SmcElement.NIL_STATE) == true)
            {
                _source.println(stateName);
            }
            else
            {
                _source.println(endStateName);
            }
        }

        // Print out the actions (if there are any). Otherwise
        // output empty braces.
        if (actions.size() == 0)
        {
            _source.println("  {}");
        }
        else
        {
            _source.println("  {");

            _indent = "    ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            _source.println("  }");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits HTML table code for this FSM action.
     * @param action emits HTML table code for this action.
     */
    public void visit(SmcAction action)
    {
        List<String> arguments = action.getArguments();

        _source.print(_indent);
        _source.print(action.getName());
        if (action.isProperty() == true)
        {
            _source.print(" = ");
            _source.print(arguments.get(0).trim());
        }
        else
        {
            Iterator<String> it;
            String sep;

            _source.print("(");

            for (it = arguments.iterator(), sep = "";
                 it.hasNext() == true;
                 sep = ", ")
            {
                _source.print(sep);
                _source.print((it.next()).trim());
            }

            _source.print(")");
        }
        _source.println(";");

        return;
    } // end of visit(SmcAction)

    /**
     * Emits HTML table code for this transition parameter.
     * @param parameter emits HTML table code for this transition
     * parameter.
     */
    public void visit(SmcParameter parameter)
    {
        _source.print(parameter.getType());
        return;
    } // end of visit(SmcParameter)

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//
} // end of class SmcTableGenerator

//
// CHANGE LOG
// $Log: SmcTableGenerator.java,v $
// Revision 1.6  2010/03/05 21:29:53  fperrad
// Allows property with Groovy, Lua, Perl, Python, Ruby & Scala
//
// Revision 1.5  2010/03/03 19:18:40  fperrad
// fix property with Graph & Table
//
// Revision 1.4  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.3  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.2  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//
