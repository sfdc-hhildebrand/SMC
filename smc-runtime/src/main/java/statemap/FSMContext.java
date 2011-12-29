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
// The Original Code is  State Machine Compiler(SMC).
// 
// The Initial Developer of the Original Code is Charles W. Rapp.
// Portions created by Charles W. Rapp are
// Copyright (C) 2000 - 2009. Charles W. Rapp.
// All Rights Reserved.
// 
// Contributor(s): 
//
// statemap.java --
//
//  This package defines the FSMContext class which must be
//  inherited by any Java class wanting to use an smc-generated
//  state machine.
//
// RCS ID
// $Id: FSMContext.java,v 1.15 2011/11/20 14:58:33 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package statemap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for all SMC-generated application context classes.
 * This class stores the FSM name, current and previous states,
 * the state stack, debugging information and state change
 * listeners.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class FSMContext
    implements Serializable
{
//---------------------------------------------------------------
// Member functions
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a finite state machine context for the given
     * initial state.
     * @param initState the finite state machine's start state.
     */
    protected FSMContext(State initState)
    {
        _state = initState;
        _transition = "";
        _previousState = null;
    } // end of FSMContext(State)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Abstract method declarations.
    //

    /**
     * Starts the finite state machine running by executing the
     * initial state's entry actions.
     */
    public abstract void enterStartState();

    //
    // end of Abstract method declarations.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Serializable Interface Implementation.
    //

    private void readObject(ObjectInputStream istream)
        throws IOException,
               ClassNotFoundException
    {
        istream.defaultReadObject();

        return;
    } // end of readObject(ObjectInputStream)

    //
    // end of Serializable Interface Implementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the logger
     * @return the Logger
     */
    abstract public Logger getLog();

    /**
     * Returns {@code true} if this FSM is in a transition and
     * {@code false} otherwise.
     * @return {@code true} if this FSM is in a transition and
     * {@code false} otherwise.
     */
    public boolean isInTransition()
    {
        return(_state == null ? true : false);
    } // end of isInTransition()

    // NOTE: getState() is defined in the SMC-generated
    // FSMContext subclass.
    
    /**
     * If this FSM is in transition, then returns the previous
     * state which the last transition left.
     * @return the previous state which the current transition
     * left. May return {@code null}.
     */
    public State getPreviousState()
        throws NullPointerException
    {
        return(_previousState);
    } // end of getPreviousState()

    /**
     * If this FSM is in transition, then returns the transition
     * name. If not in transition, then returns an empty string.
     * @return the current transition name.
     */
    public String getTransition()
    {
        return(_transition);
    } // end of getTransition()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the current state to the given value.
     * @param state The current state.
     */
    public void setState(State state)
    {
        if (getLog().isLoggable(Level.FINER))
        {
            getLog().finer(String.format("ENTER STATE     : %s [%s]",
                                     state.getName(), _name));
        }

        // clearState() is not called when a transition has
        // no actions, so set _previousState to _state in
        // that situation. We know clearState() was not
        // called when _state is not null.
        if (_state != null)
        {
            _previousState = _state;
        }

        _state = state;

        return;
    } // end of setState(State)

    /**
     * Places the current state into the previous state sets
     * the current state to {@code null}.
     */
    public void clearState()
    {
        _previousState = _state;
        _state = null;

        return;
    } // end of clearState()

    public void pushState(State state) {
        throw new UnsupportedOperationException("Push support has not been generated for this FSM Context");
        }

    public void popState() {
        throw new UnsupportedOperationException("Push support has not been generated for this FSM Context");
        }
    public void emptyStateStack() {
        throw new UnsupportedOperationException("Push support has not been generated for this FSM Context");
        }

    //
    // end of Set methods.
    //-----------------------------------------------------------

    // The following methods allow listeners to watch this
    // finite state machine for state changes.
    // Note: if a transition does not cause a state change,
    // then no state change event is fired.

    @Override
    public String toString() {
        return String.format("%s[name=%s, current=%s, previous=%s, transition=%s]", getClass().getName(), _name, _state, _previousState, _transition);
    }
    
    public String getName() {
        return _name;
    }
    
    public void setName(String name) {
        _name = name;
    }

//---------------------------------------------------------------
// Member data
//

    /**
     * The FSM name.
     */
    transient protected String _name;

    /**
     * The current state. Will be {@code null} while in
     * transition.
     */
    transient protected State _state;

    /**
     * The current transition name. Used for debugging
     * purposes. Will be en empty string when not in
     * transition.
     */
    transient protected String _transition;

    /**
     * Stores which state a transition left. May be {@code null}.
     */
    transient protected State _previousState;

    //-----------------------------------------------------------
    // Constants.
    //
    private static final long serialVersionUID = 0x060000L;
} // end of class FSMContext

//
// CHANGE LOG
// $Log: FSMContext.java,v $
// Revision 1.15  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.14  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.13  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.12  2009/03/27 09:41:07  cwrapp
// Added F. Perrad changes back in.
//
// Revision 1.11  2009/03/01 18:20:40  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.10  2008/01/14 19:59:23  cwrapp
// Release 5.0.2 check-in.
//
// Revision 1.9  2007/08/05 13:00:34  cwrapp
// Version 5.0.1 check-in. See net/sf/smc/CODE_README.txt for more information.
//
// Revision 1.8  2007/02/21 13:50:59  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.7  2005/05/28 18:44:13  cwrapp
// Updated C++, Java and Tcl libraries, added CSharp, Python
// and VB.
//
// Revision 1.1  2005/02/21 19:03:38  charlesr
// Variable name clean up.
//
// Revision 1.0  2003/12/14 20:38:40  charlesr
// Initial revision
//
