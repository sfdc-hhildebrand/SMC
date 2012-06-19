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
// $Id: SmcMap.java,v 1.5 2011/11/20 14:58:33 cwrapp Exp $
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
 * SMC has states grouped into maps. This class contains
 * {@link net.sf.smc.model.SmcState states} in a list and
 * stores the default state separately (if there is one).
 *
 * @see net.sf.smc.model.SmcFSM
 * @see net.sf.smc.model.SmcState
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcMap
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a state machine map with the given name, line
     * number and finite state machine.
     */
    public SmcMap(final String name,
                  final int lineNumber,
                  final SmcFSM fsm)
    {
        super (name, lineNumber);

        _fsm = fsm;
        _defaultState = null;
        _states = new ArrayList<SmcState>();
        _stateId = 0;
    } // end of SmcMap(String, int, SmcFSM)

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
    public void accept(final SmcVisitor visitor)
    {
        visitor.visit(this);
        return;
    } // end of accept(SmcVisitor)

    //
    // end of SmcElement Abstract Methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the owning finite state machine.
     * @return the owning finite state machine.
     */
    public SmcFSM getFSM()
    {
        return (_fsm);
    } // end of getFSM()

    /**
     * Returns the map's states.
     * @return the map's states.
     */
    public List<SmcState> getStates()
    {
        return(_states);
    } // end of getSattes()

    /**
     * Returns {@code true} if the state is in the list and
     * {@code false} otherwise.
     * @return {@code true} if the state is in the list and
     * {@code false} otherwise.
     */
    public boolean findState(final SmcState state)
    {
        SmcState state2;
        Iterator<SmcState> it;
        boolean retval;

        for (it = _states.iterator(), retval = false;
             it.hasNext() == true && retval == false;
            )
        {
            state2 = it.next();
            if (state.getInstanceName().equals(
                    state2.getInstanceName()) == true)
            {
                retval = true;
            }
        }

        return(retval);
    } // end of findState(SmcState)

    /**
     * Returns {@code true} if this map contains the nsamed
     * state; {@code false} otherwise.
     * @return {@code true} if this map contains the nsamed
     * state; {@code false} otherwise.
     */
    public boolean isKnownState(final String stateName)
    {
        SmcState state;
        Iterator<SmcState> it;
        boolean retval;

        if (stateName.compareToIgnoreCase("default" ) == 0 )
        {
            retval = this.hasDefaultState();
        }
        else
        {
            for (it = _states.iterator(), retval = false;
                 it.hasNext() == true && retval == false;
                )
            {
                state = it.next();
                retval =
                    stateName.equals(state.getInstanceName());
            }
        }
        
        return (retval);
    } // end of isKnownState(String)

    /**
     * Returns {@code true} if this map has an explicitly
     * defined default state and {@code false} otherwise.
     * @return {@code true} if this map has an explicitly
     * defined default state and {@code false} otherwise.
     */
    public boolean hasDefaultState()
    {
        return (_defaultState == null ? false : true);
    } // end of hasDefaultState()

    /**
     * Returns the default state. May return {@code null}.
     * @return the default state.
     */
    public SmcState getDefaultState()
    {
        return (_defaultState);
    } // end of getDefaultState()

    /**
     * Returns all states in this map including the default
     * state.
     * @return all states in this map including the default
     * state.
     */
    public List<SmcState> getAllStates()
    {
        List<SmcState> retval = new ArrayList<SmcState>(_states);

        if (_defaultState != null)
        {
            retval.add(_defaultState);
        }

        return (retval);
    } // end of getAllStates()

    /**
     * Returns all the transitions from all the states in this
     * map.
     * @return all the transitions from all the states in this
     * map.
     */
    public List<SmcTransition> getTransitions()
    {
        List<SmcTransition> transList;
        List<SmcTransition> retval;

        // If this map has a default state, then initialize the
        // transition list to the default state's transitions.
        // Otherwise, set it to the empty list.
        if (_defaultState != null)
        {
            retval =
                new ArrayList<SmcTransition>(
                    _defaultState.getTransitions());
        }
        else
        {
            retval = new ArrayList<SmcTransition>();
        }

        // Get each state's transition list and merge it into the
        // results.
        for (SmcState state: _states)
        {
            transList = state.getTransitions();
            retval =
                merge(transList,
                      retval,
                      new Comparator<SmcTransition>()
                      {
                          public int compare(SmcTransition o1,
                                             SmcTransition o2)
                          {
                              return(o1.compareTo(o2));
                          }
                      });
        }

        return(retval);
    } // end of getTransitions()

    /**
     * Returns the list of transitions that do not appear in the
     * default state.
     * @return the list of transitions that do not appear in the
     * default state.
     */
    public List<SmcTransition> getUndefinedDefaultTransitions()
    {
        List<SmcTransition> retval =
            new ArrayList<SmcTransition>();
        List<SmcTransition> definedDefaultTransitions;

        if (_defaultState == null)
        {
            definedDefaultTransitions =
                new ArrayList<SmcTransition>();
        }
        else
        {
            definedDefaultTransitions =
                    _defaultState.getTransitions();
            Collections.sort(
                definedDefaultTransitions,
                new Comparator<SmcTransition>()
                {
                    public int compare(SmcTransition o1,
                                       SmcTransition o2)
                    {
                        return(o1.compareTo(o2));
                    }
                });
        }

        // Make a transitions list in all the states.
        // For each transition that is *not* defined in the
        // default state, create a default definition for that
        // transition.
        for (SmcState state: _states)
        {
            for (SmcTransition transition:
                     state.getTransitions())
            {
                // Create the default transition only if it is
                // not already in the default transition list.
                // DO NOT ADD TRANSITIONS NAMED "DEFAULT".
                if (transition.getName().equals(
                        "Default") != false &&
                    definedDefaultTransitions.contains(
                        transition) == false &&
                    retval.contains(transition) == false)
                {
                    retval.add(transition);
                }
            }
        }

        return(retval);
    } // getUndefinedDefaultTransitions()

    /**
     * Returns {@code true} if at least one of the map's states
     * has an entry action and {@code false} otherwise.
     * @return {@code true} if at least one of the map's states
     * has an entry action and {@code false} otherwise.
     */
    public boolean hasEntryActions()
    {
        List<SmcAction> actions;

        for (SmcState state: _states)
        {
            actions = state.getEntryActions();
            if (actions != null && actions.isEmpty() == false)
            {
                return true;
            }
        }
        return false;
    } // end of hasEntryActions()

    /**
     * Returns {@code true} if at least one of the map's states
     * has an exit action and {@code false} otherwise.
     * @return {@code true} if at least one of the map's states
     * has an exit action and {@code false} otherwise.
     */
    public boolean hasExitActions()
    {
        List<SmcAction> actions;

        for (SmcState state: _states)
        {
            actions = state.getExitActions();
            if (actions != null && actions.isEmpty() == false)
            {
                return true;
            }
        }
        return false;
    } // end of hasExitActions()

    /**
     * Returns the next unique state identifier.
     * @return the next unique state identifier.
     */
    public int getNextStateId()
    {
        return (_stateId++);
    } // end of getNextStateId()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Adds a state to the list.
     * @param state an FSM state.
     */
    public void addState(SmcState state)
    {
        if (state.getInstanceName().compareTo(
                "DefaultState") == 0)
        {
            _defaultState = state;
        }
        else
        {
            _states.add(state);
        }

        return;
    } // end of addState(SmcStatus)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Returns the map's text representation.
     * @return the map's text representation.
     */
    @Override
    public String toString()
    {
        String retval;

        retval = "%map " + _name;
        if (_defaultState != null)
        {
            retval += "\n" + _defaultState;
        }

        for (SmcState state: _states)
        {
            retval += "\n" + state;
        }

        return(retval);
    } // end of toString()

//---------------------------------------------------------------
// Member data
//

    private SmcFSM _fsm;
    private List<SmcState> _states;
    private SmcState _defaultState;

    // Use this to generate unique state IDs.
    private int _stateId;
} // end of class SmcMap

//
// CHANGE LOG
// $Log: SmcMap.java,v $
// Revision 1.5  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.4  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.3  2009/03/27 09:41:47  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.2  2009/03/03 17:28:53  kgreg99
// 1. Bugs resolved:
// #2657779 - modified SmcParser.sm and SmcParserContext.java
// #2648516 - modified SmcCSharpGenerator.java
// #2648472 - modified SmcSyntaxChecker.java
// #2648469 - modified SmcMap.java
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.11  2008/08/15 22:21:35  fperrad
// + add method getAllStates
//
// Revision 1.10  2008/02/08 08:46:02  fperrad
// C : optimize footprint when no Entry action or no Exit action
//
// Revision 1.9  2007/02/21 13:55:52  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.8  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.7  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.6  2005/11/07 19:34:54  cwrapp
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
// Revision 1.5  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:36:20  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/03 16:46:46  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class now extends
// SmcElement.
//
// Revision 1.3  2004/10/30 16:06:07  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.2  2004/09/06 16:40:32  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:55:06  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:04:18  charlesr
// Initial revision
//
