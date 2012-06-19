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
// Copyright (C) 2000 - 2005, 2008. Charles W. Rapp.
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
// $Id: SmcParser.java,v 1.10 2011/11/20 16:29:53 cwrapp Exp $
//
// CHANGE LOG
// (See bottom of file.)
//

package net.sf.smc.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.smc.model.SmcAction;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcFSM;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcMap;
import net.sf.smc.model.SmcParameter;
import net.sf.smc.model.SmcState;
import net.sf.smc.model.SmcTransition;

/**
 * Reads in a finite state machine definition from an input
 * stream and returns the
 * {@link net.sf.smc.model.SmcFSM FSM} model. If
 * {@link #parse} throws an exception, then call
 * {@link #getMessages} for a list of parser warning and error
 * messages which explain the problems found with the
 * FSM definition. A new parser instance must be instantiated for
 * each unique input stream. A parser instance cannot be reused
 * for a different stream.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcParser
{
//---------------------------------------------------------------
// Member Methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a parser for the named FSM in the given input
     * stream. If <code>debugFlag</code> is <code>true</code>,
     * then the parser and lexer debug output will be generated.
     * @param name the finite state machine's name.
     * @param istream the input stream contains the SMC code.
     * @param targetLanguage Generates code for this target
     * language.
     * @param debugFlag if true, turn on debug output.
     */
    public SmcParser(String name,
                     InputStream istream,
                     TargetLanguage targetLanguage,
                     boolean debugFlag)
    {
        _name = name;
        _targetLanguage = targetLanguage;
        _messages = new ArrayList<SmcMessage>();
        _lexer = new SmcLexer(istream, debugFlag);
        _parserFSM = new SmcParserContext(this);
        _parserFSM.setDebugFlag(debugFlag);
    } // end of SmcParser(...)

    //
    // Constructors.
    //-----------------------------------------------------------

    /**
     * Parses the named FSM in the given input stream and returns
     * the finite state machine. If this method throws an
     * exception, then call {@link #getMessages} for a list of
     * parser warnings and errors which explain the problems
     * found in the FSM defintion.
     * @return the parser FSM model.
     * @exception IOException
     * if there is a problem reading the input stream.
     * @exception IllegalAccessException
     * if there is a problem accessing the input stream.
     * @exception InvocationTargetException
     * if there is a parse error.
     */
    public SmcFSM parse()
        throws IOException,
               IllegalAccessException,
               InvocationTargetException
    {
        SmcLexer.Token token = null;
        int tokenType;
        Object[] params = new Object[1];

        _mapInProgress = null;
        _stateInProgress = null;
        _transitionName = null;
        _transitionInProgress = null;
        _guardInProgress = null;
        _paramInProgress = null;
        _actionInProgress = null;
        _argInProgress = null;

        _paramList = null;
        _actionList = null;
        _argList = null;

        _parseStatus = true;
        _quitFlag = false;

        _fsm = new SmcFSM(_name,
                          _targetLanguage.targetFileName(_name));

        // Start lexing in cooked mode.
        _lexer.setCookedMode();

        // Read all the tokens into a list.
        tokenType = SmcLexer.TOKEN_NOT_SET;
        while (_quitFlag == false &&
               (token = _lexer.nextToken()) != null)
        {
            tokenType = token.getType();
            _lineNumber = token.getLineNumber();

            // Is the token type valid?
            if (tokenType <= SmcLexer.TOKEN_NOT_SET &&
                tokenType >= SmcLexer.TOKEN_COUNT)
            {
                // No.
                error("Undefined token type (" +
                      Integer.toString(tokenType) +
                      ")",
                      token.getLineNumber());

                _quitFlag = true;
                _parseStatus = false;
            }
            // If the last token is a failure, don't go on.
            else if (tokenType == SmcLexer.DONE_FAILED)
            {
                _quitFlag = true;
                _parseStatus = false;
                error(token.getValue(), token.getLineNumber());
            }
            // If the last token is success, don't go on either.
            else if (tokenType == SmcLexer.DONE_SUCCESS)
            {
                _quitFlag = true;
            }
            else
            {
                // Issue a transition for this token.
                params[0] = token;
                _TransMethod[tokenType].invoke(_parserFSM,
                                                params);
            }
        }

        // If the parse failed, delete the tree.
        if (_parseStatus == false)
        {
            _fsm = null;
        }

        return (_fsm);
    } // end of parse()

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the parser's warning and error messages list.
     * @return the parser's warning and error messages list.
     */
    public List<SmcMessage> getMessages()
    {
        return (_messages);
    } // end of getMessages()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // State Machine Guards
    //

    /* package */ boolean isValidHeader()
    {
        String context = _fsm.getContext();
        String start = _fsm.getStartState();

        return (context != null &&
                context.length() > 0 &&
                start != null &&
                start.length() > 0);
    } // end of isValidHeader()

    /* package */ boolean isValidStartState(String name)
    {
        int index;
        boolean retval = false;

        // The name must be of the form "<id>::<id>".
        index = name.indexOf("::");

        // Fail if "::" does not appear at all or appears
        // more than once.
        if (index >= 0 && name.indexOf("::", (index + 1)) < 0)
        {
            // Given how the lexer works, we are guaranteed
            // that the two identifiers are valid.
            retval = true;
        }

        return (retval);
    } // end of isValidStartState(String)

    /* package */ boolean isDuplicateMap(String name)
    {
        return (_fsm.findMap(name) == null ?
                false : true);
    } // end of isDuplicateMap(String)

    /* package */ boolean isDuplicateState(String name)
    {
        return (_mapInProgress.isKnownState(name));
    } // end of isDuplicateState(String)

    //
    // end of State Machine Guards
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // State Machine Actions
    //

    /* package */ void warning(String errorMsg, int lineNumber)
    {
        _messages.add(
            new SmcMessage(_name,
                           lineNumber,
                           SmcMessage.WARNING,
                           errorMsg));
        return;
    } // end of warning(String, int)

    /* package */ void error(String errorMsg, int lineNumber)
    {
        _messages.add(
            new SmcMessage(_name,
                           lineNumber,
                           SmcMessage.ERROR,
                           errorMsg));

        _parseStatus = false;

        return;
    } // end of error(String, int)

    /* package */ int getLineNumber()
    {
        return (_lineNumber);
    }

    /* package */ TargetLanguage getTargetLanguage()
    {
        return (_targetLanguage);
    }

	// THIS METHOD WAS ADDED BY kgreg99 ONLY TO RESOLVE COMPILATION ERROR
	// IT HAS TO BE EVALUATED
    // Put the lexer into raw mode.
    /* package */ void setRawMode(String openChar,
                                  String closeChar,
                                  String dummy )
    {
        _lexer.setRawMode(openChar.charAt(0),
                          closeChar.charAt(0) );
        return;
    } // end of setRawMode(String, String)

    // Put the lexer into raw mode.
    /* package */ void setRawMode(String openChar,
                                  String closeChar)
    {
        _lexer.setRawMode(openChar.charAt(0),
                          closeChar.charAt(0));
        return;
    } // end of setRawMode(String, String)

    // Put the lexer into the raw mode used for
    // collecting parameter types.
    /* package */ void setRawMode2()
    {
        _lexer.setRawMode(OPEN_CLAUSE_LIST,
                          CLOSE_CLAUSE_LIST,
                          QUOTE_LIST,
                          ')',
                          ',');

        return;
    } // end of setRawMode2()

    // Put the lexer into the raw mode used for collecting
    // parameter types.
    /* package */ void setRawMode(String closeChars)
    {
        _lexer.setRawMode(closeChars);
        return;
    } // end of setRawMode(String)

    // Put the lexer into cooked mode.
    /* package */ void setCookedMode()
    {
        _lexer.setCookedMode();
        return;
    } // end of setCookedMode()

    /* package */ void setHeaderLine(int lineNumber)
    {
        _fsm.setHeaderLine(lineNumber);
        return;
    } // end of setHeaderList(int)

    /* package */ void setSource(String source)
    {
        String src = _fsm.getSource();

        if (src != null && src.length() > 0)
        {
            warning("%{ %} source previously specified, new " +
                    "source ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setSource(source);
        }

        return;
    } // end of setSource(String)

    /* package */ void setStartState(String stateName)
    {
        String start = _fsm.getStartState();

        if (start != null && start.length() > 0)
        {
            warning("%start previously specified, new start " +
                    "state ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setStartState(stateName);
        }

        return;
    }

    /* package */ void setContext(String name)
    {
        String context = _fsm.getContext();

        if (context != null && context.length() > 0)
        {
            warning("%class previously specified, new context " +
                    "ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setContext(name);
        }

        return;
    }

    /* package */ void setFsmClassName(String name)
    {
        _fsm.setFsmClassName(name.trim());
        return;
    }
    /* package */ void setPackageName(String name)
    {
        String pkg = _fsm.getPackage();

        if (pkg != null && pkg.length() > 0)
        {
            warning("%package previously specified, " +
                    "new package ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setPackage(name.trim());
        }

        return;
    }

	
    /* package */ void addImport(String name)
    {
        _fsm.addImport(name.trim());
        return;
    }

    /* package */ void addDeclare(String name)
    {
        _fsm.addDeclare(name.trim());
        return;
    }

    /* package */ void setHeader(String name)
    {
        String header = _fsm.getHeader();

        if (header != null && header.length() > 0)
        {
            warning("%header previously specified, " +
                    "new header file ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setHeader(name.trim());
        }

        return;
    }

    /* package */ void addInclude(String name)
    {
        _fsm.addInclude(name.trim());
        return;
    }

    /* package */ void setAccessLevel(String level)
    {
        String accessLevel = _fsm.getAccessLevel();

        if (accessLevel != null && accessLevel.length() > 0)
        {
            warning("%access previously specified, " +
                    "new access level ignored.",
                    _lineNumber);
        }
        else
        {
            _fsm.setAccessLevel(level.trim());
        }

        return;
    }

    /* package */ void addMap()
    {
        if (_mapInProgress == null)
        {
            error("There is no in-progress map to add",
                  _lineNumber);
        }
        else
        {
            // If this map does not have a default state, then
            // create one now.
            if (_mapInProgress.hasDefaultState() == false)
            {
                SmcState DefaultState =
                    new SmcState(
                        "Default",
                        _mapInProgress.getLineNumber(),
                        _mapInProgress);

                _mapInProgress.addState(DefaultState);
            }

            _fsm.addMap(_mapInProgress);
            _mapInProgress = null;
        }

        return;
    }

    /* package */ void createMap(String name, int lineNumber)
    {
        if (_mapInProgress != null)
        {
            error("Cannot create new map while still filling " +
                  "in previous map (" +
                  _mapInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else
        {
	    	// check FSM class name
	    	if ( _fsm.getFsmClassName() == "" )
	    	{
	    		// set default FSM class name
	    		_fsm.setFsmClassName( _fsm.getContext()+"Context" );
	    	}
            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("CREATE MAP   : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            _mapInProgress =
                new SmcMap(name, lineNumber, _fsm);
        }

        return;
    }

    /* package */ void addState()
    {
        if (_mapInProgress == null)
        {
            error("There is no in-progress map to which the " +
                  "state may be added.",
                  _lineNumber);
        }
        else if (_stateInProgress == null)
        {
            error("There is no in-progrss state to add to the " +
                  "map.",
                  _lineNumber);
        }
        else
        {
            _mapInProgress.addState(_stateInProgress);
            _stateInProgress = null;
        }

        return;
    }

    /* package */ void createState(String name, int lineNumber)
    {
        SmcState retval;

        if (_stateInProgress != null)
        {
            error("Cannot create new state while still " +
                  "filling in previous state (" +
                  _stateInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else
        {
            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("CREATE STATE : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            _stateInProgress =
                new SmcState(name, lineNumber, _mapInProgress);
        }

        return;
    }

    /* package */ void setEntryAction(List<SmcAction> actions)
    {
        // Verify there is an in-progress state.
        if (_stateInProgress == null)
        {
            error("There is no in-progress state to receive " +
                  "the entry action.",
                  _lineNumber);
        }
        else if (_stateInProgress.getEntryActions() != null)
        {
            warning("Entry action previously specified, new " +
                    "entry action ignored.",
                    _lineNumber);
        }
        else
        {
            _stateInProgress.setEntryActions(actions);
        }

        return;
    }

    /* package */ void setExitAction(List<SmcAction> actions)
    {
        // Verify there is an in-progress state.
        if (_stateInProgress == null)
        {
            error("There is no in-progress state to receive " +
                  "the exit action.",
                  _lineNumber);
        }
        else if (_stateInProgress.getExitActions() != null)
        {
            warning("Exit action previously specified, new " +
                    "exit action ignored.",
                    _lineNumber);
        }
        else
        {
            _stateInProgress.setExitActions(actions);
        }

        return;
    }

    // Append the in-progress transition to the in-progress
    // state's transition list.
    /* package */ void addTransition()
    {
        if (_stateInProgress == null)
        {
            error("There is no in-progress state to which the " +
                  "transition may be added.",
                  _lineNumber);
        }
        else if (_transitionInProgress == null)
        {
            error("There is no in-progress transition to add " +
                  "to the state.",
                  _lineNumber);
        }
        else
        {
            _stateInProgress.addTransition(
                _transitionInProgress);
            _transitionInProgress = null;
        }

        return;
    }

    // Returns the stored transition name.
    /* package */ String getTransitionName()
    {
        return (_transitionInProgress != null ?
                _transitionInProgress.getName() :
                _transitionName);
    }

    // Store away the transition's name for later use in
    // creating the transition.
    /* package */ void storeTransitionName(String name)
    {
        if (_transitionName != null)
        {
            error("There already is a previously stored " +
                  "transition name - \"" +
                  name +
                  "\".",
                  _lineNumber);
        }
        else
        {
            _transitionName = name;
        }

        return;
    }

    // Create a transition object with the current token as its
    // name.
    /* package */ void
        createTransition(List<SmcParameter> params,
                         int lineNumber)
    {
        if (_transitionInProgress != null)
        {
            error("Cannot create new transition while still " +
                  "filling in previous transition (" +
                  _transitionInProgress.getName() +
                  ").",
                  lineNumber);
        }
        else if (_stateInProgress == null)
        {
            error("There is no in-progress state to which the " +
                  "transition may be added.",
                  lineNumber);
        }
        else if (_transitionName == null)
        {
            error("There is no stored transition name.",
                  lineNumber);
        }
        else
        {
            String name;
            SmcTransition transition;

            // Check if this state already has a transition with
            // this name. If so, then reuse that object.
            // Otherwise, create a new transition object.
            _transitionInProgress =
                _stateInProgress.findTransition(
                    _transitionName, params);
            if (_transitionInProgress == null)
            {
                if (_parserFSM.getDebugFlag() == true)
                {
                    PrintStream os = _parserFSM.getDebugStream();
                    Iterator<SmcParameter> pit;
                    String sep;
                    StringBuffer buffer = new StringBuffer(80);

                    buffer.append("CREATE TRANS : ");
                    buffer.append(_transitionName);
                    buffer.append('(');

                    for (pit = params.iterator(), sep = "";
                         pit.hasNext() == true;
                         sep = ", ")
                    {
                        buffer.append(sep);
                        buffer.append(pit.next());
                    }

                    buffer.append(')');
                    buffer.append(" (");
                    buffer.append(lineNumber);
                    buffer.append(")");

                    os.println(buffer);
                }

                _transitionInProgress =
                    new SmcTransition(
                        _transitionName,
                        params,
                        lineNumber,
                        _stateInProgress);
            }

            _transitionName = null;
        }

        return;
    }

    // Create a transition object with the current token as its
    // name.
    /* package */ void createTransition(int lineNumber)
    {
        createTransition(new ArrayList<SmcParameter>(),
                         lineNumber);
        return;
    }

    /* package */ void addGuard()
    {
        if (_transitionInProgress == null)
        {
            error("There is no in-progress transition to " +
                  "which the guard may be added.",
                  _lineNumber);
        }
        else if (_guardInProgress == null)
        {
            error("There is no in-progress guard to add " +
                  "to the " +
                  _transitionInProgress.getName() +
                  " transition.",
                  _lineNumber);
        }
        else
        {
            _transitionInProgress.addGuard(_guardInProgress);
            _guardInProgress = null;
        }

        return;
    }

    // Create a guard object with the in-progress action as its
    // condition.
    /* package */ void createGuard(String transition,
                                   String condition,
                                   int lineNumber)
    {
        if (_guardInProgress != null)
        {
            error("Cannot create new guard while still " +
                  "filling in previous guard.",
                  lineNumber);
        }
        else
        {
            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("CREATE GUARD : " +
                           condition +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            _guardInProgress =
                new SmcGuard(condition,
                             lineNumber,
                             _transitionInProgress);
        }

        return;
    }

    // Set the in-progress guard's transtion type (set, push or
    // pop).
    /* package */ void setTransType(TransType trans_type)
    {
        if (_guardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "set the transition type.",
                  _lineNumber);
        }
        else
        {
            switch (trans_type)
            {
                case TRANS_SET:
                case TRANS_PUSH:
                case TRANS_POP:
                    _guardInProgress.setTransType(trans_type);
                    break;

                default:
                    error("Transition type must be either " +
                          "\"TRANS_SET\", \"TRANS_PUSH\" or " +
                          "\"TRANS_POP\".",
                          _lineNumber);
                    break;
            }
        }

        return;
    }

    // Set the in-progress guard's end state.
    /* package */ void setEndState(String state)
    {
        if (_guardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the end state.",
                  _lineNumber);
        }
        else
        {
            _guardInProgress.setEndState(state);
        }

        return;
    }

    // Set the in-progress guard's push state.
    /* package */ void setPushState(String name)
    {
        if (_guardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the end state.",
                  _lineNumber);
        }
        else if (_guardInProgress.getTransType() !=
                     TransType.TRANS_PUSH)
        {
            error("Cannot set push state on a non-push " +
                  "transition.",
                  _lineNumber);
        }
        else if (name.equals("nil") == true)
        {
            error("Cannot push to \"nil\" state.",
                  _lineNumber);
        }
        else
        {
            _guardInProgress.setPushState(name);
        }

        return;
    }

    // Set the guard's actions.
    /* package */ void setActions(List<SmcAction> actions)
    {
        if (_guardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the action.",
                  _lineNumber);
        }
        else
        {
            _guardInProgress.setActions(actions);
        }

        return;
    }

    /* package */ void setPopArgs(String args)
    {
        if (_guardInProgress == null)
        {
            error("There is no in-progress guard to which to " +
                  "add the action.",
                  _lineNumber);
        }
        else
        {
            _guardInProgress.setPopArgs(args);
        }

        return;
    }

    /* package */ void createParamList()
    {
        if (_paramList == null)
        {
            _paramList = new ArrayList<SmcParameter>();
        }

        return;
    }

    /* package */ List<SmcParameter> getParamList()
    {
        List<SmcParameter> retval = _paramList;

        _paramList = null;

        return (retval);
    }

    /* package */ void createParameter(String name, int lineNumber)
    {
        if (_paramInProgress != null)
        {
            error("Cannot create new parameter while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            String type = "";

            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("CREATE PARAM : " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            // While Tcl is weakly typed, it still differentiates
            // between call-by-value and call-by-name.
            // By default, SMC generates call-by-value.
            if (_targetLanguage == TargetLanguage.TCL)
            {
                type = SmcParameter.TCL_VALUE_TYPE;
            }

            _paramInProgress =
                new SmcParameter(name, lineNumber, type);
        }

        return;
    }

    /* package */ void setParamType(String type)
    {
        if (_paramInProgress == null)
        {
            error("There is no in-progress parameter to which " +
                  "to add the type.",
                  _lineNumber);
        }
        else
        {
            _paramInProgress.setType(type);
        }

        return;
    }

    /* package */ void addParameter()
    {
        if (_paramList == null)
        {
            error("There is no parameter list to which the " +
                  "parameter may be added.",
                  _lineNumber);
        }
        else if (_paramInProgress == null)
        {
            error("There is no in-progress parameter to add " +
                  "to the list.",
                  _lineNumber);
        }
        else
        {
            _paramList.add(_paramInProgress);
            _paramInProgress = null;
        }

        return;
    }

    /* package */ void clearParameter()
    {
        _paramInProgress = null;
        return;
    }

    /* package */ void createActionList()
    {
        if (_actionList != null)
        {
            error("Cannot create an action list when one " +
                  "already exists.",
                  _lineNumber);
        }
        else
        {
            _actionList = new ArrayList<SmcAction>();
        }

        return;
    }

    /* package */ List<SmcAction> getActionList()
    {
        List<SmcAction> retval = _actionList;

        _actionList = null;

        return(retval);
    }

    /* package */ void createAction(String name, int lineNumber)
    {
        if (_actionInProgress != null)
        {
            error("Cannot create new action while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("CREATE ACTION: " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            _actionInProgress =
                new SmcAction(name, lineNumber);
        }

        return;
    }

    /* package */ void setActionArgs(List<String> args)
    {
        if (_actionInProgress == null)
        {
            error("There is no in-progress action to which to " +
                  "add the arguments.",
                  _lineNumber);
        }
        else
        {
            _actionInProgress.setArguments(args);
        }

        return;
    }

    /* package */ void addAction()
    {
        if (_actionList == null)
        {
            error("There is no action list to which the " +
                  "action may be added.",
                  _lineNumber);
        }
        else if (_actionInProgress == null)
        {
            error("There is no in-progress action to add to " +
                  "the list.",
                  _lineNumber);
        }
        else
        {
            _actionList.add(_actionInProgress);
            _actionInProgress = null;
        }

        return;
    }

    // Retrieve the current action's property assignment flag.
    /* package */ boolean getProperty()
    {
        boolean retcode = false;

        if (_actionInProgress == null)
        {
            error("There is no in-progress action, " +
                  "get property flag failed.",
                  _lineNumber);
        }
        else
        {
            retcode = _actionInProgress.isProperty();
        }

        return (retcode);
    }

    // Mark the current action as a .Net assignment.
    /* package */ void setProperty(boolean flag)
    {
        if (_actionInProgress == null)
        {
            error("There is no in-progress action, " +
                  "set property flag failed.",
                  _lineNumber);
        }
        else
        {
            _actionInProgress.setProperty(flag);
        }

        return;
    }

    /* package */ void clearActions()
    {
        if (_actionList != null)
        {
            _actionList.clear();
            _actionList = null;
        }

        return;
    }

    /* package */ void createArgList()
    {
        if (_argList != null)
        {
            error("Cannot create an argument list when one " +
                  "already exists.",
                  _lineNumber);
        }
        else
        {
            _argList = new ArrayList<String>();
        }

        return;
    }

    /* package */ List<String> getArgsList()
    {
        List<String> retval = _argList;

        _argList = null;

        return(retval);
    }

    /* package */ void
        createArgument(String name, int lineNumber)
    {
        if (_argInProgress != null)
        {
            error("Cannot create new argument while still " +
                  "filling in previous one.",
                  lineNumber);
        }
        else
        {
            if (_parserFSM.getDebugFlag() == true)
            {
                PrintStream os = _parserFSM.getDebugStream();

                os.println("   CREATE ARG: " +
                           name +
                           "(" +
                           Integer.toString(lineNumber) +
                           ")");
            }

            _argInProgress = name;
        }

        return;
    } // end of createArgument(String, int)

    /* package */ void addArgument()
    {
        if (_argList == null)
        {
            error("There is no argument list to which the " +
                  "argument may be added.",
                  _lineNumber);
        }
        else if (_argInProgress == null)
        {
            error("There is no in-progress argument to add to " +
                  "the list.",
                  _lineNumber);
        }
        else
        {
            _argList.add(_argInProgress.trim());
            _argInProgress = null;
        }

        return;
    } // end of addArgument()

    /* package */ void clearArguments()
    {
        if (_argList != null)
        {
            _argList.clear();
            _argList = null;
        }

        return;
    } // end of clearArguments()

    //
    // end of State Machine Actions
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Enums.
//

    /**
     * The currently supported SMC target languages. This enum
     * is here because the parser allows different features in
     * the FSM defintion based on the target language.
     */
    public enum TargetLanguage
    {
        /**
         * The target language is undefined.
         */
        LANG_NOT_SET (""),

        /**
         * <a href="http://www.research.att.com/~bs/C++.html">C++</a>
         */
        C_PLUS_PLUS ("{0}_sm"),

        /**
         * <a href="http://java.sun.com">Java</a>
         */
        JAVA ("{0}Context"),

        /**
         * <a href="http://www.tcl.tk">Tcl</a>
         */
        TCL ("{0}_sm"),

        /**
         * <a href="http://msdn.microsoft.com/en-us/vbasic/default.aspx">VB.net</a>
         */
        VB ("{0}_sm"),

        /**
         * .<a href="http://msdn.microsoft.com/en-us/vcsharp/default.aspx">net C#</a>
         */
        C_SHARP ("{0}_sm"),

        /**
         * <a href="http://www.python.org">Python</a>
         */
        PYTHON ("{0}_sm"),

        /**
         * An HTML table
         */
        TABLE ("{0}_sm"),

        /**
         * <a href="http://www.graphviz.org">GraphViz</a>
         */
        GRAPH ("{0}_sm"),

        /**
         * <a href="http://www.perl.org">Perl</a>
         */
        PERL ("{0}_sm"),

        /**
         * <a href="http://ruby-lang.org">Ruby</a>
         */
        RUBY ("{0}_sm"),

        /**
         * C
         */
        C ("{0}_sm"),

        /**
         * Objective C
         */
        OBJECTIVE_C ("{0}_sm"),

        /**
         * <a href="http://www.lua.org">Lua</a>
         */
        LUA ("{0}_sm"),

        /**
         * <a href="http://groovy.codehaus.org">Groovy</a>
         */
        GROOVY ("{0}Context"),

        /**
         * <a href="http://www.scala-lang.org">Scala</a>
         */
        SCALA ("{0}Context"),

        /**
         * <a href="http://www.php.net">PHP</a>
         */
        PHP ("{0}_sm"),

        /**
         * JavaScript
         */
        JS ("{0}_sm");

    //-----------------------------------------------------------
    // Member methods.
    //

        //-------------------------------------------------------
        // Constructors.
        //

        private TargetLanguage(final String format)
        {
            _sourceNameFormat = format;
        } // end of TargetLanguage(String)

        //
        // end of Constructors.
        //-------------------------------------------------------

        // Returns the source file name based on the class name.
        public String targetFileName(final String className)
        {
            return (
                MessageFormat.format(
                    _sourceNameFormat, className));
        } // end of targetFileName(String)

    //-----------------------------------------------------------
    // Member data.
    //

        // The default source file name pattern.
        private final String _sourceNameFormat;
    } // end of enum TargetLanguage

    /**
     * SMC currently supports 17 different target languages plus
     * one for an error code.
     */
    public static final int LANGUAGE_COUNT = 18;

//---------------------------------------------------------------
// Member Data
//

    // The FSM name.
    private final String _name;

    // The target language affects how the SMC code is parsed.
    private final TargetLanguage _targetLanguage;

    // Store warning and error messages into this list. Do not
    // output them. That is up to the application.
    private List<SmcMessage> _messages;

    // The parse state map.
    private SmcParserContext _parserFSM;

    // Get tokens from the lexer.
    private SmcLexer _lexer;

    // Keep track of errors.
    private boolean _parseStatus;
    private boolean _quitFlag;

    // Store the parse result here.
    private SmcFSM _fsm;

    private SmcMap _mapInProgress;
    private SmcState _stateInProgress;
    private String _transitionName;
    private SmcTransition _transitionInProgress;
    private SmcGuard _guardInProgress;
    private SmcParameter _paramInProgress;
    private SmcAction _actionInProgress;
    private String _argInProgress;

    // Store parsed parameters here.
    private List<SmcParameter> _paramList;

    // Store parsed transition actions here.
    private List<SmcAction> _actionList;

    // Store parsed action arguments here.
    private List<String> _argList;

    private int _lineNumber;

    //-----------------------------------------------------------
    // Statics.
    //

    // List of characters which open and clause subexpressions.
    private static List<Character> OPEN_CLAUSE_LIST;
    private static List<Character> CLOSE_CLAUSE_LIST;
    private static List<Character> QUOTE_LIST;

    // Create a hashmap which associates token names with
    // parser transitions. When a token is received, use this
    // table to get the appropriate transition method and
    // invoke that method.
    private static Method[] _TransMethod;

    static
    {
        String transName = "<not set>";

        OPEN_CLAUSE_LIST = new ArrayList<Character>();
        CLOSE_CLAUSE_LIST = new ArrayList<Character>();
        QUOTE_LIST = new ArrayList<Character>();

        OPEN_CLAUSE_LIST.add(new Character('('));
        OPEN_CLAUSE_LIST.add(new Character('{'));
        OPEN_CLAUSE_LIST.add(new Character('['));
        OPEN_CLAUSE_LIST.add(new Character('<'));

        CLOSE_CLAUSE_LIST.add(new Character(')'));
        CLOSE_CLAUSE_LIST.add(new Character('}'));
        CLOSE_CLAUSE_LIST.add(new Character(']'));
        CLOSE_CLAUSE_LIST.add(new Character('>'));

        QUOTE_LIST.add(new Character('"'));
        QUOTE_LIST.add(new Character('\''));

        _TransMethod = new Method[SmcLexer.TOKEN_COUNT];

        try
        {
            Class<SmcParserContext> fsmClass =
                SmcParserContext.class;
            Class[] paramTypes = new Class[1];

            paramTypes[0] = SmcLexer.Token.class;

            transName = "ENTRY";
            _TransMethod[SmcLexer.ENTRY] =
                fsmClass.getDeclaredMethod("ENTRY",
                                           paramTypes);
            transName = "EXIT";
            _TransMethod[SmcLexer.EXIT] =
                fsmClass.getDeclaredMethod("EXIT",
                                           paramTypes);
            transName = "JUMP";
            _TransMethod[SmcLexer.JUMP] =
                fsmClass.getDeclaredMethod("JUMP",
                                           paramTypes);
            transName = "POP";
            _TransMethod[SmcLexer.POP] =
                fsmClass.getDeclaredMethod("POP",
                                           paramTypes);
            transName = "PUSH";
            _TransMethod[SmcLexer.PUSH] =
                fsmClass.getDeclaredMethod("PUSH",
                                           paramTypes);
            transName = "WORD";
            _TransMethod[SmcLexer.WORD] =
                fsmClass.getDeclaredMethod("WORD",
                                           paramTypes);
            transName = "START_STATE";
            _TransMethod[SmcLexer.START_STATE] =
                fsmClass.getDeclaredMethod("START_STATE",
                                           paramTypes);
            transName = "MAP_NAME";
            _TransMethod[SmcLexer.MAP_NAME] =
                fsmClass.getDeclaredMethod("MAP_NAME",
                                           paramTypes);
            transName = "CLASS_NAME";
            _TransMethod[SmcLexer.CLASS_NAME] =
                fsmClass.getDeclaredMethod("CLASS_NAME",
                                           paramTypes);
            transName = "HEADER_FILE";
            _TransMethod[SmcLexer.HEADER_FILE] =
                fsmClass.getDeclaredMethod("HEADER_FILE",
                                           paramTypes);
            transName = "INCLUDE_FILE";
            _TransMethod[SmcLexer.INCLUDE_FILE] =
                fsmClass.getDeclaredMethod("INCLUDE_FILE",
                                           paramTypes);
            transName = "PACKAGE_NAME";
            _TransMethod[SmcLexer.PACKAGE_NAME] =
                fsmClass.getDeclaredMethod("PACKAGE_NAME",
                                           paramTypes);
            transName = "FSM_CLASS_NAME";
            _TransMethod[SmcLexer.FSM_CLASS_NAME] =
                fsmClass.getDeclaredMethod("FSM_CLASS_NAME",
                                           paramTypes);
            transName = "IMPORT";
            _TransMethod[SmcLexer.IMPORT] =
                fsmClass.getDeclaredMethod("IMPORT",
                                           paramTypes);
            transName = "DECLARE";
            _TransMethod[SmcLexer.DECLARE] =
                fsmClass.getDeclaredMethod("DECLARE",
                                           paramTypes);
            transName = "LEFT_BRACE";
            _TransMethod[SmcLexer.LEFT_BRACE] =
                fsmClass.getDeclaredMethod("LEFT_BRACE",
                                           paramTypes);
            transName = "RIGHT_BRACE";
            _TransMethod[SmcLexer.RIGHT_BRACE] =
                fsmClass.getDeclaredMethod("RIGHT_BRACE",
                                           paramTypes);
            transName = "LEFT_BRACKET";
            _TransMethod[SmcLexer.LEFT_BRACKET] =
                fsmClass.getDeclaredMethod("LEFT_BRACKET",
                                           paramTypes);
            transName = "LEFT_PAREN";
            _TransMethod[SmcLexer.LEFT_PAREN] =
                fsmClass.getDeclaredMethod("LEFT_PAREN",
                                           paramTypes);
            transName = "RIGHT_PAREN";
            _TransMethod[SmcLexer.RIGHT_PAREN] =
                fsmClass.getDeclaredMethod("RIGHT_PAREN",
                                           paramTypes);
            transName = "COMMA";
            _TransMethod[SmcLexer.COMMA] =
                fsmClass.getDeclaredMethod("COMMA",
                                           paramTypes);
            transName = "COLON";
            _TransMethod[SmcLexer.COLON] =
                fsmClass.getDeclaredMethod("COLON",
                                           paramTypes);
            transName = "SEMICOLON";
            _TransMethod[SmcLexer.SEMICOLON] =
                fsmClass.getDeclaredMethod("SEMICOLON",
                                           paramTypes);
            transName = "SOURCE";
            _TransMethod[SmcLexer.SOURCE] =
                fsmClass.getDeclaredMethod("SOURCE",
                                           paramTypes);
            transName = "EOD";
            _TransMethod[SmcLexer.EOD] =
                fsmClass.getDeclaredMethod("EOD",
                                           paramTypes);
            transName = "SLASH";
            _TransMethod[SmcLexer.SLASH] =
                fsmClass.getDeclaredMethod("SLASH",
                                           paramTypes);
            transName = "EQUAL";
            _TransMethod[SmcLexer.EQUAL] =
                fsmClass.getDeclaredMethod("EQUAL",
                                           paramTypes);
            transName = "ACCESS";
            _TransMethod[SmcLexer.ACCESS] =
                fsmClass.getDeclaredMethod("ACCESS",
                                           paramTypes);
            transName = "DOLLAR";
            _TransMethod[SmcLexer.DOLLAR] =
                fsmClass.getDeclaredMethod("DOLLAR",
                                           paramTypes);
        }
        catch (NoSuchMethodException ex1)
        {
            System.err.println("INITIALIZATION ERROR! No such " +
                               "method as SmcParserContext." +
                               transName +
                               ".");
            System.exit(2);
        }
        catch (SecurityException ex2)
        {
            System.err.println("INITIALIZATION ERROR! Not " +
                               "allowed to access " +
                               "SmcParserContext." +
                               transName +
                               ".");
            System.exit(2);
        }
    } // end of static
} // end of class SmcParser

//
// CHANGE LOG
// $Log: SmcParser.java,v $
// Revision 1.10  2011/11/20 16:29:53  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.9  2011/02/14 21:29:56  nitin-nizhawan
// corrected some build errors
//
// Revision 1.7  2010/02/15 18:05:44  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.6  2009/11/27 19:44:39  cwrapp
// Correct TargetLanguage.GRAPH source file name definition.
//
// Revision 1.5  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.4  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.3  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.2  2009/04/11 13:11:13  cwrapp
// Corrected raw mode 3 to handle multiple argument template/generic declarations.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.16  2007/11/19 18:53:21  fperrad
// + add : jump syntax
//   jump uses the same syntax as push,
//   allows transition between states of different maps but without stacking a return context.
//
// Revision 1.15  2007/02/21 13:56:09  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.14  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.13  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.12  2005/11/07 19:34:54  cwrapp
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
// Revision 1.11  2005/09/14 01:51:33  cwrapp
// Changes in release 4.2.0:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (Java) -java broken due to an untested minor change.
//
// Revision 1.10  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.9  2005/07/07 12:11:04  fperrad
// Add a new token '$' for Perl language.
//
// Revision 1.8  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.7  2005/06/18 18:28:42  cwrapp
// SMC v. 4.0.1
//
// New Features:
//
// (No new features.)
//
// Bug Fixes:
//
// + (C++) When the .sm is in a subdirectory the forward- or
//   backslashes in the file name are kept in the "#ifndef" in the
//   generated header file. This is syntactically wrong. SMC now
//   replaces the slashes with underscores.
//
// + (Java) If %package is specified in the .sm file, then the
//   generated *Context.java class will have package-level access.
//
// + The Programmer's Manual had incorrect HTML which prevented the
//   pages from rendering correctly on Internet Explorer.
//
// + Rewrote the Programmer's Manual section 1 to make it more
//   useful.
//
// Revision 1.6  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.5  2005/02/21 15:37:43  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.4  2005/02/21 15:19:30  charlesr
// Trimming import, header and include names because they are lexed
// as pure source.
//
// Revision 1.3  2005/02/03 17:04:39  charlesr
// The parser was modified as part of an ongoing project to
// make the SMC parser a self-contained, stand-alone library.
// These changes include:
// + Have SmcParser instantiate the SmcLexer thereby making
//   the lexer entirely encapsulated by the parser.
// + Collecting warning and error messages in SmcMessage
//   objects. The application owning the parser can then call
//   SmcParser.getMessages() and display the messages in the
//   appropriate manner.
// + If the parser is in debug mode, then all output is now
//   guaranteed to be written to the same output stream.
//
// Revision 1.2  2004/09/06 16:41:03  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:56:05  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:05:08  charlesr
// Initial revision
//
