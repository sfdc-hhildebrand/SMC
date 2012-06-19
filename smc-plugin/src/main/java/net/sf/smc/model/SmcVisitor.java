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
// $Id: SmcVisitor.java,v 1.1 2009/03/01 18:20:42 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

/**
 * The super class for all FSM model visitors. Within the
 * SMC applications, visitors perform either global syntax
 * checking on the tree or generate code based on the tree.
 * See the Visitor pattern in GoF (p. 331).
 * <p>
 * If an application needs to traverse a finite state machine
 * model, then it should create a class derived from
 * SmcVisitor, override the appropriate visit methods for
 * those FSM elements it needs to process. Visiting is
 * started by calling {@link net.sf.smc.model.SmcFSM#accept} and
 * passing in the application's visitor instance. See
 * {@link net.sf.smc.generator.SmcCodeGenerator} for an
 * example.
 * <p>
 * All visit methods are explicity defined and do nothing.
 * The reason why there are not abstract is because not all
 * visistors visit all element types. Therefore, Visitor
 * subclasses need override only those methods visiting
 * the elements they care about.
 *
 * @see SmcElement
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class SmcVisitor
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Default constructor. Does nothing since this abstract
     * class has no member data to initialize.
     */
    protected SmcVisitor()
    {}

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Visitor methods.
    //

    /**
     * Visits the top-level finite state machine element.
     * @param fsm The top-level finite state machine element.
     */
    public void visit(SmcFSM fsm)
    {}

    /**
     * Visits a finite state machine submap.
     * @param map A finite state machine submap.
     */
    public void visit(SmcMap map)
    {}

    /**
     * Visits a finite state machine state.
     * @param state A finite state machine state.
     */
    public void visit(SmcState state)
    {}

    /**
     * Visits a finite state machine state transition.
     * @param transition A finite state machine state transition.
     */
    public void visit(SmcTransition transition)
    {}

    /**
     * Visits a state tansition guard.
     * @param guard A state tansition guard.
     */
    public void visit(SmcGuard guard)
    {}

    /**
     * Visits a state transition action.
     * @param action A state transition action.
     */
    public void visit(SmcAction action)
    {}

    /**
     * Visits a state transition parameter.
     * @param parameter A state transition parameter.
     */
    public void visit(SmcParameter parameter)
    {}

    //
    // end of Visitor methods.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//
} // end of class SmcVisitor

//
// CHANGE LOG
// $Log: SmcVisitor.java,v $
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.3  2007/01/15 00:23:52  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.2  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.1  2005/05/28 19:28:43  cwrapp
// Moved to visitor pattern.
//
// Revision 1.1  2005/02/21 15:38:58  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.0  2005/02/03 17:09:19  charlesr
// Initial revision
//
