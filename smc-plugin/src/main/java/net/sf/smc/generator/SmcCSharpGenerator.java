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
// Copyright (C) 2005, 2008 - 2009. Charles W. Rapp.
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
// $Id: SmcCSharpGenerator.java,v 1.12 2011/11/20 14:58:33 cwrapp Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.io.PrintStream;
import java.util.ArrayList;
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
 * Visits the abstract syntax tree, emitting C# code to an output
 * stream.
 * @see SmcElement
 * @see SmcCodeGenerator
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcCSharpGenerator
    extends SmcCodeGenerator
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a C# code generator for the given options.
     * @param options The target code generator options.
     */
    public SmcCSharpGenerator(final SmcOptions options)
    {
        super (options, "cs");
    } // end of SmcCSharpGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    /**
     * Emits C# code for the finite state machine.
     * Generates the using statements, namespace, the FSM
     * context class, the default transitions and the FSM map
     * classes.
     * @param fsm emit C# code for this finite state machine.
     */
    public void visit(SmcFSM fsm)
    {
        String rawSource = fsm.getSource();
        String packageName = fsm.getPackage();
        String context = fsm.getContext();
        String fsmClassName = fsm.getFsmClassName();
        String startState = fsm.getStartState();
        String accessLevel = fsm.getAccessLevel();
        List<SmcMap> maps = fsm.getMaps();
        List<SmcTransition> transitions;
        Iterator<SmcParameter> pit;
        String transName;
        String csState;
        String separator;
        int index;
        List<SmcParameter> params;
        String indent2;

        // If the access level has not been set, then the
        // default is "public".
        if (accessLevel == null || accessLevel.length() == 0)
        {
            accessLevel = "public";
        }

        // Dump out the raw source code, if any.
        if (rawSource != null && rawSource.length() > 0)
        {
            _source.println(rawSource);
            _source.println();
        }

        // Always include the system package.
        _source.println("using System;");

        // If debugging code is being generated, then import
        // system diagnostics package as well.
        if (_debugLevel >= DEBUG_LEVEL_0)
        {
            _source.println("using System.Diagnostics;");
        }

        // If serialization is on, then import the .Net
        // serialization package.
        if (_serialFlag == true)
        {
            _source.println(
                "using System.Runtime.Serialization;");
            _source.println("using System.Security;");
            _source.println(
                "using System.Security.Permissions;");
        }

        // If reflection is on, then import the .Net collections
        // package.
        if (_reflectFlag == true)
        {
            if (_genericFlag == true)
            {
                _source.println("using System.Collections.Generic;");
            }
            else
            {
                _source.println("using System.Collections;");
            }
        }
        _source.println();

        // Do user-specified imports now.
        for (String imp: fsm.getImports())
        {
            _source.print("using ");
            _source.print(imp);
            _source.println(";");
        }

        // If a package has been specified, generate the package
        // statement now and set the indent.
        if (packageName != null && packageName.length() > 0)
        {
            _source.print("namespace ");
            _source.println(packageName);
            _source.println("{");
            _indent = "    ";
        }

        // Does the user want to serialize this FSM?
        if (_serialFlag == true)
        {
            _source.print(_indent);
            _source.println("[Serializable]");
        }

        // Now declare the FSM context class.
        _source.print(_indent);
        _source.print(accessLevel);
        _source.print(" sealed class ");
        _source.print(fsmClassName);
        _source.println(" :");
        _source.print(_indent);
        _source.print("    statemap.FSMContext");
        if (_serialFlag == false)
        {
            _source.println();
        }
        else
        {
            _source.println(',');
            _source.print(_indent);
            _source.println("    ISerializable");
        }
        _source.print(_indent);
        _source.println("{");
        _source.print(_indent);
        _source.println(
            "//---------------------------------------------------------------");
        _source.print(_indent);
        _source.println("// Properties.");
        _source.print(_indent);
        _source.println("//");
        _source.println();

        // State property.
        _source.print(_indent);
        _source.print("    public ");
        _source.print(context);
        _source.println("State State");
        _source.print(_indent);
        _source.println("    {");
        _source.print(_indent);
        _source.println("        get");
        _source.print(_indent);
        _source.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            lock (this)");
            _source.print(_indent);
            _source.println("            {");

            indent2 = _indent + "                ";
        }
        else
        {
            indent2 = _indent + "            ";
        }

        _source.print(indent2);
        _source.println("if (state_ == null)");
        _source.print(indent2);
        _source.println("{");
        _source.print(indent2);
        _source.println("    throw(");
        _source.print(indent2);
        _source.println(
            "        new statemap.StateUndefinedException());");
        _source.print(indent2);
        _source.println("}");
        _source.println();
        _source.print(indent2);
        _source.print("return ((");
        _source.print(context);
        _source.println("State) state_);");

        // If we are in a lock block, close it.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            }");
        }

        // Close the State get.
        _source.print(_indent);
        _source.println("        }");

        // Now generate the State set.
        _source.print(_indent);
        _source.println("        set");
        _source.print(_indent);
        _source.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            lock (this)");
            _source.print(_indent);
            _source.println("            {");

            indent2 = _indent + "                ";
        }
        else
        {
            indent2 = _indent + "            ";
        }

        _source.print(indent2);
        _source.println("SetState(value);");

        // If we are in a lock block, close it.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            }");
        }

        // Close the State set.
        _source.print(_indent);
        _source.println("        }");

        // Close the state property.
        _source.print(_indent);
        _source.println("    }");
        _source.println();

        // Generate the Owner property.
        _source.print(_indent);
        _source.print("    public ");
        _source.print(context);
        _source.println(" Owner");
        _source.print(_indent);
        _source.println("    {");

        // Generate the property get method.
        _source.print(_indent);
        _source.println("        get");
        _source.print(_indent);
        _source.println("        {");
        _source.print(_indent);
        _source.println("            return (_owner);");
        _source.print(_indent);
        _source.println("        }");

        // Generate the property set method.
        _source.print(_indent);
        _source.println("        set");
        _source.print(_indent);
        _source.println("        {");

        // Again, if synchronization is on, then protect access
        // to this FSM.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            lock (this)");
            _source.print(_indent);
            _source.println("            {");

            indent2 = _indent + "                ";
        }
        else
        {
            indent2 = _indent + "            ";
        }

        _source.print(indent2);
        _source.println("_owner = value;");

        // If we are in a lock block, close it.
        if (_syncFlag == true)
        {
            _source.print(_indent);
            _source.println("            }");
        }

        // Close the Onwer set.
        _source.print(_indent);
        _source.println("        }");

        // Close the Owner property.
        _source.print(_indent);
        _source.println("    }");
        _source.println();

        // If reflect is on, then generate the States property.
        if (_reflectFlag == true)
        {
            _source.print(_indent);
            _source.print("    public ");
            _source.print(context);
            _source.println("State[] States");
            _source.print(_indent);
            _source.println("    {");

            // Generate the property get method. There is no set
            // method.
            _source.print(_indent);
            _source.println("        get");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println("            return (_States);");
            _source.print(_indent);
            _source.println("        }");

            // Close the States property.
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }

        _source.print(_indent);
        _source.println(
            "//---------------------------------------------------------------");
        _source.print(_indent);
        _source.println("// Member methods.");
        _source.print(_indent);
        _source.println("//");
        _source.println();

        // The state name "map::state" must be changed to
        // "map.state".
        if ((index = startState.indexOf("::")) >= 0)
        {
            csState = startState.substring(0, index) +
                      "." +
                      startState.substring(index + 2);
        }
        else
        {
            csState = startState;
        }

        // Generate the context class' constructor.
        _source.print(_indent);
        _source.print("    public ");
        _source.print(fsmClassName);
        _source.print("(");
        _source.print(context);
        _source.println(" owner) :");
        _source.print(_indent);
        _source.print("        base (");
        _source.print(csState);
        _source.println(")");
        _source.print(_indent);
        _source.println("    {");
        _source.println("        _owner = owner;");
        _source.print(_indent);
        _source.println("    }");
        _source.println();

        // The finite state machine start method.
        _source.print(_indent);
        _source.println("    public override void EnterStartState()");
        _source.print(_indent);
        _source.println("    {");
        _source.print(_indent);
        _source.println("        State.Entry(this);");
        _source.print(_indent);
        _source.println("        return;");
        _source.print(_indent);
        _source.println("    }");
        _source.println();

        // If -serial was specified, then generate the
        // deserialize constructor.
        if (_serialFlag == true)
        {
            _source.print(_indent);
            _source.print("    public ");
            _source.print(fsmClassName);
            _source.print("(SerializationInfo info, ");
            _source.println("StreamingContext context) :");
            _source.print(_indent);
            _source.println("        base ()");
            _source.print(_indent);
            _source.println("    {");
            _source.print(_indent);
            _source.println("        int stackSize;");
            _source.print(_indent);
            _source.println("        int stateId;");
            _source.println();
            _source.print(_indent);
            _source.print(
                "        stackSize = ");
            _source.println("info.GetInt32(\"stackSize\");");
            _source.print(_indent);
            _source.println("        if (stackSize > 0)");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println("            int index;");
            _source.print(_indent);
            _source.println("            String name;");
            _source.println();
            _source.print(_indent);
            _source.print(
                "            for (index = (stackSize - 1); ");
            _source.println("index >= 0; --index)");
            _source.print(_indent);
            _source.println("            {");
            _source.print(_indent);
            _source.print("                ");
            _source.println("name = \"stackIndex\" + index;");
            _source.print(_indent);
            _source.print("                ");
            _source.println("stateId = info.GetInt32(name);");
            _source.print(_indent);
            _source.print("                ");
            _source.println("PushState(_States[stateId]);");
            _source.print(_indent);
            _source.println("            }");
            _source.print(_indent);
            _source.println("        }");
            _source.println();
            _source.print(_indent);
            _source.println(
                "        stateId = info.GetInt32(\"state\");");
            _source.print(_indent);
            _source.println(
                "        PushState(_States[stateId]);");
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }

        // Generate the default transition methods.
        // First get the transition list.
        transitions = fsm.getTransitions();
        for (SmcTransition trans: transitions)
        {
            transName = trans.getName();

            // Ignore the default transition.
            if (transName.equals("Default") == false)
            {
                _source.print(_indent);
                _source.print("    public void ");
                _source.print(transName);
                _source.print("(");

                // Now output the transition's parameters.
                params = trans.getParameters();
                for (pit = params.iterator(), separator = "";
                     pit.hasNext() == true;
                     separator = ", ")
                {
                    _source.print(separator);
                    (pit.next()).accept(this);
                }
                _source.println(")");
                _source.print(_indent);
                _source.println("    {");

                // If the -sync flag was specified, then output
                // "lock (this)" to prevent multiple threads from
                // access this state machine simultaneously.
                if (_syncFlag == true)
                {
                    _source.print(_indent);
                    _source.println("        lock (this)");
                    _source.print(_indent);
                    _source.println("        {");

                    indent2 = _indent + "            ";
                }
                else
                {
                    indent2 = _indent + "        ";
                }

                // Save away the transition name in case it is
                // need in an UndefinedTransitionException.
                _source.print(indent2);
                _source.print("transition_ = \"");
                _source.print(transName);
                _source.println("\";");

                _source.print(indent2);
                _source.print("State.");
                _source.print(transName);
                _source.print("(this");

                for (SmcParameter param: params)
                {
                    _source.print(", ");
                    passParameter(param);
                }
                _source.println(");");
                _source.print(indent2);
                _source.println("transition_ = \"\";");

                // If the -sync flag was specified, then output
                // the "End SyncLock".
                if (_syncFlag == true)
                {
                    _source.print(_indent);
                    _source.println("        }");
                    _source.println();
                }

                _source.print(_indent);
                _source.println("        return;");
                _source.print(_indent);
                _source.println("    }");
                _source.println();
            }
        }

        // If -serial specified, then output the valueOf(int)
        // method.
        if (_serialFlag == true)
        {
            _source.print(_indent);
            _source.print("    public ");
            _source.print(context);
            _source.println("State valueOf(int stateId)");
            _source.print(_indent);
            _source.println("    {");
            _source.print(_indent);
            _source.println("        return(_States[stateId]);");
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }

        // If serialization is turned on, then output the
        // GetObjectData method.
        if (_serialFlag == true)
        {
            _source.print(_indent);
            _source.print("    [SecurityPermissionAttribute(");
            _source.print("SecurityAction.Demand, ");
            _source.println("SerializationFormatter=true)]");
            _source.print(_indent);
            _source.print("    public void GetObjectData(");
            _source.println("SerializationInfo info,");
            _source.print(_indent);
            _source.print("                              ");
            _source.println("StreamingContext context)");
            _source.print(_indent);
            _source.println("    {");
            _source.print(_indent);
            _source.println("        int stackSize = 0;");
            _source.println();
            _source.print(_indent);
            _source.println("        if (stateStack_ != null)");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println(
                "            stackSize = stateStack_.Count;");
            _source.print(_indent);
            _source.println("        }");
            _source.println();
            _source.print(_indent);
            _source.print("        ");
            _source.println(
                "info.AddValue(\"stackSize\", stackSize);");
            _source.println();
            _source.print(_indent);
            _source.println("        if (stackSize > 0)");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println("            int index = 0;");
            _source.print(_indent);
            _source.println("            String name;");
            _source.println();
            _source.print(_indent);
            _source.print("            foreach (");
            _source.print(context);
            _source.println("State state in stateStack_)");
            _source.print(_indent);
            _source.println("            {");
            _source.print(_indent);
            _source.print("                ");
            _source.println("name = \"stackIndex\" + index;");
            _source.print(_indent);
            _source.print("                info.AddValue(");
            _source.println("name, state.Id);");
            _source.print(_indent);
            _source.println("                ++index;");
            _source.print(_indent);
            _source.println("            }");
            _source.print(_indent);
            _source.println("        }");
            _source.println();
            _source.print(_indent);
            _source.println(
                "        info.AddValue(\"state\", state_.Id);");
            _source.println();
            _source.print(_indent);
            _source.println("        return;");
            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }

        // Declare member data.
        _source.print(_indent);
        _source.println(
            "//---------------------------------------------------------------");
        _source.print(_indent);
        _source.println("// Member data.");
        _source.print(_indent);
        _source.println("//");
        _source.println();
        _source.print(_indent);
        _source.println("    [NonSerialized]");
        _source.print(_indent);
        _source.print("    private ");
        _source.print(context);
        _source.println(" _owner;");
        _source.println();

        // If serialization support is on, then create the state
        // array.
        if (_serialFlag == true || _reflectFlag == true)
        {
            String mapName;

            _source.print(_indent);
            _source.println(
                "    // Map state IDs to state objects.");
            _source.print(_indent);
            _source.println(
                "    // Used to deserialize an FSM.");
            _source.print(_indent);
            _source.println("    [NonSerialized]");
            _source.print(_indent);
            _source.print("    private static ");
            _source.print(context);
            _source.println("State[] _States =");
            _source.print(_indent);
            _source.print("    {");

            separator = "";
            for (SmcMap map: maps)
            {
                mapName = map.getName();

                for (SmcState state: map.getStates())
                {
                    _source.println(separator);
                    _source.print(_indent);
                    _source.print("        ");
                    _source.print(mapName);
                    _source.print(".");
                    _source.print(state.getClassName());

                    separator = ",";
                }
            }

            _source.println();
            _source.print(_indent);
            _source.println("    };");
            _source.println();
        }

        // Declare the inner state class.
        _source.print(_indent);
        _source.println(
            "//---------------------------------------------------------------");
        _source.print(_indent);
        _source.println("// Inner classes.");
        _source.print(_indent);
        _source.println("//");
        _source.println();
        _source.print(_indent);
        _source.print("    public abstract class ");
        _source.print(context);
        _source.println("State :");
        _source.print(_indent);
        _source.println("        statemap.State");
        _source.print(_indent);
        _source.println("    {");

        // The abstract Transitions property - if reflection was
        // is specified.
        if (_reflectFlag == true)
        {
            _source.print(_indent);
            _source.println("    //-----------------------------------------------------------");
            _source.print(_indent);
            _source.println("    // Properties.");
            _source.print(_indent);
            _source.println("    //");
            _source.println();
            _source.print(_indent);
            _source.print("        ");
            _source.print("public abstract IDictionary");
            if (_genericFlag == true)
            {
                _source.print("<string, int>");
            }
            _source.println(" Transitions");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println("            get;");
            _source.print(_indent);
            _source.println("        }");
            _source.println();
        }

        _source.print(_indent);
        _source.println("    //-----------------------------------------------------------");
        _source.print(_indent);
        _source.println("    // Member methods.");
        _source.print(_indent);
        _source.println("    //");
        _source.println();

        // State constructor.
        _source.print(_indent);
        _source.print("        internal ");
        _source.print(context);
        _source.println("State(string name, int id) :");
        _source.print(_indent);
        _source.println("            base (name, id)");
        _source.print(_indent);
        _source.println("        {}");
        _source.println();

        // Entry/Exit methods.
        _source.print(_indent);
        _source.print(
            "        protected internal virtual void Entry(");
        _source.print(fsmClassName);
        _source.println(" context)");
        _source.print(_indent);
        _source.println("        {}");
        _source.println();
        _source.print(_indent);
        _source.print(
            "        protected internal virtual void Exit(");
        _source.print(fsmClassName);
        _source.println(" context)");
        _source.print(_indent);
        _source.println("        {}");
        _source.println();

        // Transition methods (except default).
        for (SmcTransition trans: transitions)
        {
            transName = trans.getName();

            if (transName.equals("Default") == false)
            {
                _source.print(_indent);
                _source.print(
                    "        protected internal virtual void ");
                _source.print(transName);
                _source.print("(");
                _source.print(fsmClassName);
                _source.print(" context");

                for (SmcParameter param: trans.getParameters())
                {
                    _source.print(", ");
                    param.accept(this);
                }

                _source.println(")");
                _source.print(_indent);
                _source.println("        {");

                // If this method is reached, that means this
                // transition was passed to a state which does
                // not define the transition. Call the state's
                // default transition method.
                _source.print(_indent);
                _source.println("            Default(context);");

                _source.print(_indent);
                _source.println("        }");
                _source.println();
            }
        }

        // Generate the overall Default transition for all maps.
        _source.print(_indent);
        _source.print(
            "        protected internal virtual void Default(");
        _source.print(fsmClassName);
        _source.println(" context)");
        _source.print(_indent);
        _source.println("        {");

        // If generating debug code, then write this trace
        // message.
        if (_debugLevel >= DEBUG_LEVEL_0)
        {
            _source.println("#if TRACE");
            _source.print(_indent);
            _source.println("            Trace.WriteLine(");
            _source.print(_indent);
            _source.print(
                "                \"TRANSITION : Default\"");
            _source.println(");");
            _source.println("#endif");
        }

        // The default transition action is to throw a
        // TransitionUndefinedException.
        _source.print(_indent);
        _source.println("            throw (");
        _source.print(_indent);
        _source.print("                ");
        _source.println(
            "new statemap.TransitionUndefinedException(");
        _source.print(_indent);
        _source.println(
            "                    \"State: \" +");
        _source.print(_indent);
        _source.println(
            "                    context.State.Name +");
        _source.print(_indent);
        _source.println(
            "                    \", Transition: \" +");
        _source.print(_indent);
        _source.println(
            "                    context.GetTransition()));");

        // Close the Default transition method.
        _source.print(_indent);
        _source.println("        }");

        // Close the inner state class declaration.
        _source.print(_indent);
        _source.println("    }");

        // Have each map print out its source code now.
        for (SmcMap map: maps)
        {
            map.accept(this);
        }

        // Close the context class.
        _source.print(_indent);
        _source.println("}");
        _source.println();

        // If a package has been specified, then generate
        // the closing brace now.
        if (packageName != null && packageName.length() > 0)
        {
            _source.println("}");
        }

        return;
    } // end of visit(SmcFSM)

    /**
     * Emits C# code for the FSM map.
     * @param map emit C# code for this map.
     */
    public void visit(SmcMap map)
    {
        List<SmcTransition> definedDefaultTransitions;
        SmcState defaultState = map.getDefaultState();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String indent2;
        List<SmcState> states = map.getStates();

        // Initialize the default transition list to all the
        // default state's transitions.
        if (defaultState != null)
        {
            definedDefaultTransitions =
                defaultState.getTransitions();
        }
        else
        {
            definedDefaultTransitions =
                new ArrayList<SmcTransition>();
        }

        // Declare the map class and make it abstract to prevent
        // its instantiation.
        _source.println();
        _source.print(_indent);
        _source.print("    internal abstract class ");
        _source.println(mapName);
        _source.print(_indent);
        _source.println("    {");
        _source.print(_indent);
        _source.println(
            "    //-----------------------------------------------------------");
        _source.print(_indent);
        _source.println("    // Member methods.");
        _source.print(_indent);
        _source.println("    //");
        _source.println();
        _source.print(_indent);
        _source.println(
            "    //-----------------------------------------------------------");
        _source.print(_indent);
        _source.println("    // Member data.");
        _source.print(_indent);
        _source.println("    //");
        _source.println();
        _source.print(_indent);
        _source.println(
            "        //-------------------------------------------------------");
        _source.print(_indent);
        _source.println("        // Statics.");
        _source.print(_indent);
        _source.println("        //");

        // Declare each of the state class member data.
        for (SmcState state: states)
        {
            _source.print(_indent);
            _source.println("        [NonSerialized]");
            _source.print(_indent);
            _source.print(
                "        internal static readonly ");
            _source.print(mapName);
            _source.print("_Default.");
            _source.print(mapName);
            _source.print('_');
            _source.print(state.getClassName());
            _source.print(' ');
            _source.print(state.getInstanceName());
            _source.println(" =");
            _source.print(_indent);
            _source.print("            new ");
            _source.print(mapName);
            _source.print("_Default.");
            _source.print(mapName);
            _source.print("_");
            _source.print(state.getClassName());
            _source.print("(\"");
            _source.print(mapName);
            _source.print(".");
            _source.print(state.getClassName());
            _source.print("\", ");
            _source.print(map.getNextStateId());
            _source.println(");");
        }

        // Create the default state as well.
        _source.print(_indent);
        _source.println("        [NonSerialized]");
        _source.print(_indent);
        _source.print("        private static readonly ");
        _source.print(mapName);
        _source.println("_Default Default =");
        _source.print(_indent);
        _source.print("            new ");
        _source.print(mapName);
        _source.print("_Default(\"");
        _source.print(mapName);
        _source.println(".Default\", -1);");
        _source.println();

        // End of map class.
        _source.print(_indent);
        _source.println("    }");
        _source.println();

        // Declare the map default state class.
        _source.print(_indent);
        _source.print("    internal class ");
        _source.print(mapName);
        _source.println("_Default :");
        _source.print(_indent);
        _source.print("        ");
        _source.print(context);
        _source.println("State");
        _source.print(_indent);
        _source.println("    {");

        // If reflection is on, generate the Transition property.
        if (_reflectFlag == true)
        {
            _source.print(_indent);
            _source.println(
                "    //-----------------------------------------------------------");
            _source.print(_indent);
            _source.println("    // Properties.");
            _source.print(_indent);
            _source.println("    //");
            _source.println();
            _source.print(_indent);
            _source.print("        ");
            _source.print("public override IDictionary");
            if (_genericFlag == true)
            {
                _source.print("<string, int>");
            }
            _source.println(" Transitions");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.println("            get");
            _source.print(_indent);
            _source.println("            {");
            _source.print(_indent);
            _source.println(
                "                return (_transitions);");
            _source.print(_indent);
            _source.println("            }");
            _source.print(_indent);
            _source.println("        }");
            _source.println();
        }

        // Generate the constructor.
        _source.print(_indent);
        _source.println(
            "    //-----------------------------------------------------------");
        _source.print(_indent);
        _source.println("    // Member methods.");
        _source.print(_indent);
        _source.println("    //");
        _source.println();
        _source.print(_indent);
        _source.print("        internal ");
        _source.print(mapName);
        _source.println(
            "_Default(string name, int id) :");
        _source.print(_indent);
        _source.println("            base (name, id)");
        _source.print(_indent);
        _source.println("        {}");

        // Declare the user-defined transitions first.
        indent2 = _indent;
        _indent = _indent + "        ";
        for (SmcTransition trans: definedDefaultTransitions)
        {
            trans.accept(this);
        }
        _indent = indent2;

        // Have each state now generate its code. Each state
        // class is an inner class.
        _source.println();
        _source.print(_indent);
        _source.println(
            "    //-----------------------------------------------------------");
        _source.print(_indent);
        _source.println("    // Inner classes.");
        _source.print(_indent);
        _source.println("    //");
        for (SmcState state: states)
        {
            state.accept(this);
        }

        // If reflection is on, then define the transitions list.
        if (_reflectFlag == true)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            String transName;
            List<SmcParameter> parameters;
            int transDefinition;

            _source.println();
            _source.print(_indent);
            _source.println(
                "    //-----------------------------------------------------------");
            _source.print(_indent);
            _source.println("    // Member data.");
            _source.print(_indent);
            _source.println("    //");
            _source.println();
            _source.print(_indent);
            _source.println(
                "        //-------------------------------------------------------");
            _source.print(_indent);
            _source.println("        // Statics.");
            _source.print(_indent);
            _source.println("        //");
            _source.print(_indent);
            _source.print("        ");
            _source.print("private static IDictionary");
            if (_genericFlag == true)
            {
                _source.print("<string, int>");
            }
            _source.println(" _transitions;");
            _source.println();
            _source.print(_indent);
            _source.print("        static ");
            _source.print(mapName);
            _source.println("_Default()");
            _source.print(_indent);
            _source.println("        {");
            _source.print(_indent);
            _source.print("            ");
            _source.print("_transitions = new ");
            if (_genericFlag == true)
            {
                _source.print("Dictionary<string, int>");
            }
            else
            {
                _source.print("Hashtable");
            }
            _source.println("();");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();
                transName += "(";
                parameters = transition.getParameters();
                for ( int i = 0; i < parameters.size(); i++)
                {
                	SmcParameter singleParam= parameters.get( i );
                	transName += singleParam.getType() + " " + singleParam.getName();
                	if ( i < parameters.size() - 1 )
                	{
		                transName += ", ";
                	}
                }
                transName += ")";

                // If the transition is defined in this map's
                // default state, then the value is 2.
                if (definedDefaultTransitions.contains(
                        transition) == true)
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                _source.print("            ");
                _source.print("_transitions.Add(\"");
                _source.print(transName);
                _source.print("\", ");
                _source.print(transDefinition);
                _source.println(");");
            }
            _source.print(_indent);
            _source.println("        }");
        }

        // End of the map default state class.
        _source.print(_indent);
        _source.println("    }");

        return;
    } // end of visit(SmcMap)

    /**
     * Emits C# code for this FSM state.
     * @param state emits C# code for this state.
     */
    public void visit(SmcState state)
    {
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
		String fsmClassName = map.getFSM().getFsmClassName();
        String mapName = map.getName();
        String stateName = state.getClassName();
        List<SmcAction> actions;
        String indent2;

        // Declare the inner state class.
        _source.println();
        _source.print(_indent);
        _source.print("        internal class ");
        _source.print(mapName);
        _source.print("_");
        _source.print(stateName);
        _source.println(" :");
        _source.print(_indent);
        _source.print("            ");
        _source.print(mapName);
        _source.println("_Default");
        _source.print(_indent);
        _source.println("        {");

        // Generate the Transitions property if reflection is on.
        if (_reflectFlag == true)
        {
            _source.print(_indent);
            _source.println(
                "        //-------------------------------------------------------");
            _source.print(_indent);
            _source.println("        // Properties.");
            _source.print(_indent);
            _source.println("        //");
            _source.println();
            _source.print(_indent);
            _source.print("            ");
            _source.print("public override IDictionary");
            if (_genericFlag == true)
            {
                _source.print("<string, int>");
            }
            _source.println(" Transitions");
            _source.print(_indent);
            _source.println("            {");
            _source.print(_indent);
            _source.println("                get");
            _source.print(_indent);
            _source.println("                {");
            _source.print(_indent);
            _source.println(
                "                    return (_transitions);");
            _source.print(_indent);
            _source.println("                }");
            _source.print(_indent);
            _source.println("            }");
            _source.println();
        }

        // Add the constructor.
        _source.print(_indent);
        _source.println(
            "        //-------------------------------------------------------");
        _source.print(_indent);
        _source.println("        // Member methods.");
        _source.print(_indent);
        _source.println("        //");
        _source.println();
        _source.print(_indent);
        _source.print("            internal ");
        _source.print(mapName);
        _source.print("_");
        _source.print(stateName);
        _source.println("(string name, int id) :");
        _source.print(_indent);
        _source.println("                base (name, id)");
        _source.print(_indent);
        _source.println("            {}");

        // Add the Entry() and Exit() methods if this state
        // defines them.
        actions = state.getEntryActions();
        if (actions != null && actions.isEmpty() == false)
        {
            _source.println();
            _source.print(_indent);
            _source.print("            ");
            _source.print(
                "protected internal override void Entry(");
            _source.print(fsmClassName);
            _source.println(" context)");
            _source.print(_indent);
            _source.println("            {");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.print("                ");
            _source.print(context);
            _source.println(" ctxt = context.Owner;");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "                ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent2;

            // End of the Entry() method.
            _source.print(_indent);
            _source.println("                return;");
            _source.print(_indent);
            _source.println("            }");
        }

        actions = state.getExitActions();
        if (actions != null && actions.isEmpty() == false)
        {
            _source.println();
            _source.print(_indent);
            _source.print("            ");
            _source.print(
                "protected internal override void Exit(");
            _source.print(fsmClassName);
            _source.println(" context)");
            _source.print(_indent);
            _source.println("            {");

            // Declare the "ctxt" local variable.
            _source.print(_indent);
            _source.print("                ");
            _source.print(context);
            _source.println(" ctxt = context.Owner;");
            _source.println();

            // Generate the actions associated with this code.
            indent2 = _indent;
            _indent = _indent + "                ";
            for (SmcAction action: actions)
            {
                action.accept(this);
            }

            // End of the Exit() method.
            _source.print(_indent);
            _source.println("                return;");
            _source.print(_indent);
            _source.println("            }");
        }

        // Have each transition generate its code.
        indent2 = _indent;
        _indent = _indent + "            ";
        for (SmcTransition trans: state.getTransitions())
        {
            trans.accept(this);
        }
        _indent = indent2;

        // If reflection is on, then generate the transitions
        // map.
        if (_reflectFlag == true)
        {
            List<SmcTransition> allTransitions =
                map.getFSM().getTransitions();
            List<SmcTransition> stateTransitions =
                state.getTransitions();
            SmcState defaultState = map.getDefaultState();
            List<SmcTransition> defaultTransitions;
            String transName;
			List<SmcParameter> parameters;            
            int transDefinition;

            // Initialize the default transition list to all the
            // default state's transitions.
            if (defaultState != null)
            {
                defaultTransitions =
                    defaultState.getTransitions();
            }
            else
            {
                defaultTransitions =
                    new ArrayList<SmcTransition>();
            }

            _source.println();
            _source.print(_indent);
            _source.println(
                "        //-------------------------------------------------------");
            _source.print(_indent);
            _source.println("        // Member data.");
            _source.print(_indent);
            _source.println("        //");
            _source.println();
            _source.print(_indent);
            _source.println(
                "            //---------------------------------------------------");
            _source.print(_indent);
            _source.println("            // Statics.");
            _source.print(_indent);
            _source.println("            //");
            _source.print(_indent);
            _source.print("            ");
            _source.print("new private static IDictionary");
            if (_genericFlag == true)
            {
                _source.print("<string, int>");
            }
            _source.println(" _transitions;");
            _source.println();
            _source.print(_indent);
            _source.print("            static ");
            _source.print(mapName);
            _source.print("_");
            _source.print(stateName);
            _source.println("()");
            _source.print(_indent);
            _source.println("            {");
            _source.print(_indent);
            _source.print("                ");
            _source.print("_transitions = new ");
            if (_genericFlag == true)
            {
                _source.print("Dictionary<string, int>");
            }
            else
            {
                _source.print("Hashtable");
            }
            _source.println("();");

            // Now place the transition names into the list.
            for (SmcTransition transition: allTransitions)
            {
                transName = transition.getName();
                transName += "(";
                parameters = transition.getParameters();
                for ( int i = 0; i < parameters.size(); i++)
                {
                	SmcParameter singleParam= parameters.get( i );
                	transName += singleParam.getType() + " " + singleParam.getName();
                	if ( i < parameters.size() - 1 )
                	{
		                transName += ", ";
                	}
                }
                transName += ")";

                // If the transition is in this state, then its
                // value is 1.
                if (stateTransitions.contains(
                        transition) == true)
                {
                    transDefinition = 1;
                }
                // If the transition is defined in this map's
                // default state, then the value is 2.
                else if (defaultTransitions.contains(
                             transition) == true)
                {
                    transDefinition = 2;
                }
                // Otherwise the value is 0 - undefined.
                else
                {
                    transDefinition = 0;
                }

                _source.print("                ");
                _source.print("_transitions.Add(\"");
                _source.print(transName);
                _source.print("\", ");
                _source.print(transDefinition);
                _source.println(");");
            }

            _source.print(_indent);
            _source.println("            }");
        }

        // End of state declaration.
        _source.print(_indent);
        _source.println("        }");

        return;
    } // end of visit(SmcState)

    /**
     * Emits C# code for this FSM state transition.
     * @param transition emits C# code for this state transition.
     */
    public void visit(SmcTransition transition)
    {
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
		String fsmClassName = map.getFSM().getFsmClassName();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        List<SmcParameter> parameters =
            transition.getParameters();
        List<SmcGuard> guards = transition.getGuards();
        boolean nullCondition = false;
        Iterator<SmcGuard> git;
        SmcGuard guard;

        _source.println();
        _source.print(_indent);
        _source.print("protected internal override void ");
        _source.print(transName);
        _source.print("(");
        _source.print(fsmClassName);
        _source.print(" context");

        // Add user-defined parameters.
        for (SmcParameter param: parameters)
        {
            _source.print(", ");
            param.accept(this);
        }
        _source.println(")");

        _source.print(_indent);
        _source.println("{");

        // Almost all transitions have a "ctxt" local variable.
        if (transition.hasCtxtReference() == true)
        {
            _source.println();
            _source.print(_indent);
            _source.print("    ");
            _source.print(context);
            _source.println(" ctxt = context.Owner;");
            _source.println();
        }

        // Output transition to debug stream.
        if (_debugLevel >= DEBUG_LEVEL_0)
        {
            _source.println();
            _source.println("#if TRACE");
            _source.print(_indent);
            _source.println("    Trace.WriteLine(");
            _source.print(_indent);
            _source.print("        \"LEAVING STATE   : ");
            _source.print(mapName);
            _source.print('.');
            _source.print(stateName);
            _source.println("\");");
            _source.println("#endif");
            _source.println();
        }

        // Loop through the guards and print each one.
        _guardIndex = 0;
        _guardCount = guards.size();
        for (git = guards.iterator();
             git.hasNext() == true;
             ++_guardIndex)
        {
            guard = git.next();

            // Count up the guards with no condition.
            if (guard.getCondition().length() == 0)
            {
                nullCondition = true;
            }

            guard.accept(this);
        }

        // If all guards have a condition, then create a final
        // "else" clause which passes control to the default
        // transition. Pass all arguments into the default
        // transition.
        if (_guardIndex > 0 && nullCondition == false)
        {
            // If there was only one guard, then we need to close
            // of its body.
            if (_guardCount == 1)
            {
                _source.print(_indent);
                _source.println("}");
            }

            _source.print(_indent);
            _source.print("    else");
            _source.print(_indent);
            _source.println("    {");

            // Call the super class' transition method using
            // the "base" keyword and not the class name.
            _source.print(_indent);
            _source.print("        base.");
            _source.print(transName);
            _source.print("(context");
            for (SmcParameter param: parameters)
            {
                _source.print(", ");
                passParameter(param);
            }
            _source.println(");");

            _source.print(_indent);
            _source.println("    }");
            _source.println();
        }
        // Need to add a final newline after a multiguard block.
        else if (_guardCount > 1)
        {
            _source.println();
            _source.println();
        }

        // End of transition.
        _source.print(_indent);
        _source.println("    return;");
        _source.print(_indent);
        _source.println("}");

        return;
    } // end of visit(SmcTransition)

    /**
     * Emits C# code for this FSM transition guard.
     * @param guard emits C# code for this transition guard.
     */
    public void visit(SmcGuard guard)
    {
        SmcTransition transition = guard.getTransition();
        SmcState state = transition.getState();
        SmcMap map = state.getMap();
        String context = map.getFSM().getContext();
        String mapName = map.getName();
        String stateName = state.getClassName();
        String transName = transition.getName();
        TransType transType = guard.getTransType();
        boolean loopbackFlag = false;
        String indent2;
        String indent3;
        String indent4;
        String endStateName = guard.getEndState();
        String fqEndStateName = "";
        String pushStateName = guard.getPushState();
        String condition = guard.getCondition();
        List<SmcAction> actions = guard.getActions();
        boolean hasActions = !(actions.isEmpty());

        // If this guard's end state is not of the form
        // "map::state", then prepend the map name to the
        // state name.
        // DON'T DO THIS IF THIS IS A POP TRANSITION!
        // The "state" is actually a transition name.
        if (transType != TransType.TRANS_POP &&
            endStateName.length () > 0 &&
            endStateName.equals(SmcElement.NIL_STATE) == false)
        {
            endStateName = scopeStateName(endStateName, mapName);
        }

        // Qualify the state and push state names as well.
        stateName = scopeStateName(stateName, mapName);
        pushStateName = scopeStateName(pushStateName, mapName);

        // Is this an *internal* loopback?
        loopbackFlag = isLoopback(transType, endStateName);

        // The guard code generation is a bit tricky. The first
        // question is how many guards are there? If there are
        // more than one, then we will need to generate the
        // proper "if-then-else" code.
        if (_guardCount > 1)
        {
            indent2 = _indent + "        ";

            // There are multiple guards.
            // Is this the first guard?
            if (_guardIndex == 0 && condition.length() > 0)
            {
                // Yes, this is the first. This means an "if"
                // should be used.
                _source.print(_indent);
                _source.print("    if (");
                _source.print(condition);
                _source.println(")");
                _source.print(_indent);
                _source.println("    {");
            }
            else if (condition.length() > 0)
            {
                // No, this is not the first transition but it
                // does have a condition. Use an "else if".
                _source.println();
                _source.print(_indent);
                _source.print("    else if (");
                _source.print(condition);
                _source.println(")");
                _source.print(_indent);
                _source.println("    {");
            }
            else
            {
                // This is not the first transition and it has
                // no condition.
                _source.println();
                _source.print(_indent);
                _source.println("    else");
                _source.print(_indent);
                _source.println("    {");
            }
        }
        // There is only one guard. Does this guard have
        // a condition?
        else if (condition.length() == 0)
        {
            // No. This is a plain, old. vanilla transition.
            indent2 = _indent + "    ";
        }
        else
        {
            // Yes there is a condition.
            indent2 = _indent + "        ";

            _source.print(_indent);
            _source.print("    if (");
            _source.print(condition);
            _source.println(")");
            _source.print(_indent);
            _source.println("    {");
        }

        // Now that the necessary conditions are in place, it's
        // time to dump out the transition's actions. First, do
        // the proper handling of the state change. If this
        // transition has no actions, then set the end state
        // immediately. Otherwise, unset the current state so
        // that if an action tries to issue a transition, it will
        // fail.
        if (hasActions == false && endStateName.length() != 0)
        {
            fqEndStateName = endStateName;
        }
        else if (hasActions == true)
        {
            // Save away the current state if this is a loopback
            // transition. Storing current state allows the
            // current state to be cleared before any actions are
            // executed. Remember: actions are not allowed to
            // issue transitions and clearing the current state
            // prevents them from doing do.
            if (loopbackFlag == true)
            {
                fqEndStateName = "endState";

                _source.print(indent2);
                _source.print(context);
                _source.print("State ");
                _source.print(fqEndStateName);
                _source.println(" = context.State;");
            }
            else
            {
                fqEndStateName = endStateName;
            }
        }

        _source.println();

        // Dump out the exit actions if
        // 1) this is a standard, non-loopback transition or
        // 2) a pop transition.
        if (transType == TransType.TRANS_POP ||
            loopbackFlag == false)
        {
            if (_debugLevel >= DEBUG_LEVEL_1)
            {
                _source.println("#if TRACE");
                _source.print(indent2);
                _source.println("Trace.WriteLine(");
                _source.print(indent2);
                _source.print("    \"BEFORE EXIT     : ");
                _source.print(stateName);
                _source.println(".Exit(context)\");");
                _source.println("#endif");
                _source.println();
            }

            _source.print(indent2);
            _source.println("context.State.Exit(context);");

            if (_debugLevel >= DEBUG_LEVEL_1)
            {
                _source.println("#if TRACE");
                _source.print(indent2);
                _source.println("Trace.WriteLine(");
                _source.print(indent2);
                _source.print("    \"AFTER EXIT      : ");
                _source.print(stateName);
                _source.println(".Exit(context)\");");
                _source.println("#endif");
                _source.println();
            }
        }

        if (_debugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();
            String sep;

            _source.println("#if TRACE");
            _source.print(indent2);
            _source.println("Trace.WriteLine(");
            _source.print(indent2);
            _source.print("    \"ENTER TRANSITION: ");
            _source.print(mapName);
            _source.print('.');
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);

            // Output the transition parameters.
            _source.print("(");
            for (SmcParameter param: parameters)
            {
                _source.print(", ");
                param.accept(this);
            }
            _source.print(")");
            _source.println("\");");
            _source.println("#endif");
            _source.println();
        }

        // Dump out this transition's actions.
        if (hasActions == false)
        {
            if (condition.length() > 0)
            {
                _source.print(indent2);
                _source.println("// No actions.");
            }

            indent3 = indent2;
        }
        else
        {
            // Now that we are in the transition, clear the
            // current state.
            _source.print(indent2);
            _source.println("context.ClearState();");

            // v. 2.0.0: Place the actions inside a try/finally
            // block. This way the state will be set before an
            // exception leaves the transition method.
            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (_noCatchFlag == false)
            {
                _source.println();
                _source.print(indent2);
                _source.println("try");
                _source.print(indent2);
                _source.println("{");

                indent3 = indent2 + "    ";
            }
            else
            {
                indent3 = indent2;
            }

            indent4 = _indent;
            _indent = indent3;
            for (SmcAction action: actions)
            {
                action.accept(this);
            }
            _indent = indent4;

            // v. 2.2.0: Check if the user has turned off this
            // feature first.
            if (_noCatchFlag == false)
            {
                _source.print(indent2);
                _source.println("}");
                _source.print(indent2);
                _source.println("finally");
                _source.print(indent2);
                _source.println("{");
            }
        }

        if (_debugLevel >= DEBUG_LEVEL_0)
        {
            List<SmcParameter> parameters =
                transition.getParameters();
            String sep;

            _source.println("#if TRACE");
            _source.print(indent3);
            _source.println("Trace.WriteLine(");
            _source.print(indent3);
            _source.print("    \"EXIT TRANSITION : ");
            _source.print(mapName);
            _source.print('.');
            _source.print(stateName);
            _source.print(".");
            _source.print(transName);

            // Output the transition parameters.
            _source.print("(");
            for (SmcParameter param: parameters)
            {
                _source.print(", ");
                param.accept(this);
            }
            _source.print(")");
            _source.println("\");");
            _source.println("#endif");
            _source.println();
        }

        // Print the state assignment if necessary. Do NOT
        // generate the state assignment if:
        // 1. The transition has no actions AND is a loopback OR
        // 2. This is a push or pop transition.
        if (transType == TransType.TRANS_SET &&
            (hasActions == true || loopbackFlag == false))
        {
            _source.print(indent3);
            _source.print("context.State = ");
            _source.print(fqEndStateName);
            _source.println(";");
        }
        else if (transType == TransType.TRANS_PUSH)
        {
            // Set the next state so this it can be pushed
            // onto the state stack. But only do so if a clear
            // state was done.
            // v. 4.3.0: If the full-qualified end state is
            // "nil", then don't need to do anything.
            if ((loopbackFlag == false || hasActions == true) &&
                fqEndStateName.equals(SmcElement.NIL_STATE) == false)
            {
                _source.print(indent3);
                _source.print("context.State = ");
                _source.print(fqEndStateName);
                _source.println(";");
            }

            // Before doing the push, execute the end state's
            // entry actions (if any) if this is not a loopback.
            if (loopbackFlag == false)
            {
                if (_debugLevel >= DEBUG_LEVEL_1)
                {
                    _source.println("#if TRACE");
                    _source.print(indent3);
                    _source.println("Trace.WriteLine(");
                    _source.print(indent3);
                    _source.print("    \"BEFORE ENTRY    : ");
                    _source.print(mapName);
                    _source.print('.');
                    _source.print(stateName);
                    _source.println(".Exit(context)\");");
                    _source.println("#endif");
                    _source.println();
                }

                _source.print(indent3);
                _source.println("context.State.Entry(context);");

                if (_debugLevel >= DEBUG_LEVEL_1)
                {
                    _source.println("#if TRACE");
                    _source.print(indent3);
                    _source.println("Trace.WriteLine(");
                    _source.print(indent3);
                    _source.print("    \"AFTER ENTRY     : ");
                    _source.print(mapName);
                    _source.print('.');
                    _source.print(stateName);
                    _source.println(".Exit(context)\");");
                    _source.println("#endif");
                    _source.println();
                }
            }

            _source.print(indent3);
            _source.print("context.PushState(");
            _source.print(pushStateName);
            _source.println(");");
        }
        else if (transType == TransType.TRANS_POP)
        {
            _source.print(indent3);
            _source.println("context.PopState();");
        }

        // Perform the new state's enty actions if:
        // 1) this is a standard, non-loopback transition or
        // 2) a push transition.
        if ((transType == TransType.TRANS_SET &&
             loopbackFlag == false) ||
             transType == TransType.TRANS_PUSH)
        {
            if (_debugLevel >= DEBUG_LEVEL_1)
            {
                _source.println("#if TRACE");
                _source.print(indent3);
                _source.println("Trace.WriteLine(");
                _source.print(indent3);
                _source.print("    \"BEFORE ENTRY    : ");
                _source.print(mapName);
                _source.print('.');
                _source.print(fqEndStateName);
                _source.println(".Exit(context)\");");
                _source.println("#endif");
                _source.println();
            }

            _source.print(indent3);
            _source.println("context.State.Entry(context);");

            if (_debugLevel >= DEBUG_LEVEL_1)
            {
                _source.println("#if TRACE");
                _source.print(indent3);
                _source.println("Trace.WriteLine(");
                _source.print(indent3);
                _source.print("    \"AFTER ENTRY     : ");
                _source.print(mapName);
                _source.print('.');
                _source.print(fqEndStateName);
                _source.println(".Exit(context)\");");
                _source.println("#endif");
                _source.println();
            }
        }

        // If there was a try/finally, then put the closing
        // brace on the finally block.
        // v. 2.2.0: Check if the user has turned off this
        // feature first.
        if (hasActions == true && _noCatchFlag == false)
        {
            _source.print(indent2);
            _source.println("}");
            _source.println();
        }

        // If there is a transition associated with the pop, then
        // issue that transition here.
        if (transType == TransType.TRANS_POP &&
            endStateName.equals(SmcElement.NIL_STATE) == false &&
            endStateName.length() > 0)
        {
            String popArgs = guard.getPopArgs();

            _source.println();
            _source.print(indent2);
            _source.print("context.");
            _source.print(endStateName);
            _source.print("(");

            // Output any and all pop arguments.
            if (popArgs.length() > 0)
            {
                _source.print(popArgs);
            }
            _source.println(");");
        }

        // If this is a guarded transition, it will be necessary
        // to close off the "if" body. DON'T PRINT A NEW LINE!
        // Why? Because an "else" or "else if" may follow and we
        // won't know until we go back to the transition source
        // generator whether all clauses have been done.
        if (_guardCount > 1)
        {
            _source.print(_indent);
            _source.print("    }");
        }

        return;
    } // end of visit(SmcGuard)

    /**
     * Emits C# code for this FSM action.
     * @param action emits C# code for this action.
     */
    public void visit(SmcAction action)
    {
        String name = action.getName();
        List<String> arguments = action.getArguments();
        Iterator<String> it;
        String sep;

        _source.print(_indent);

        // Need to distinguish between FSMContext actions and
        // application class actions. If the action is
        // "emptyStateStack", then pass it to the context.
        // Otherwise, let the application class handle it.
        if ( action.isEmptyStateStack() == true)
        {
            _source.println("context.EmptyStateStack();");
        }
        else
        {
        	if ( action.isStatic() == false )
        	{
	            _source.print("ctxt.");
        	}
            _source.print(name);
	        if (action.isProperty() == true)
	        {
	            String arg = arguments.get(0);
	
	            _source.print(" = ");
	            _source.print(arg);
	            _source.println(";");
	        }
	        else
	        {
	            _source.print("(");
	
	            for (it = arguments.iterator(), sep = "";
	                 it.hasNext() == true;
	                 sep = ", ")
	            {
	                _source.print(sep);
	                _source.print(it.next());
	            }
	
	            _source.println(");");
	        }
	        }

        return;
    } // end of visit(SmcAction)

    /**
     * Emits C# code for this transition parameter.
     * @param parameter emits C# code for this transition parameter.
     */
    public void visit(SmcParameter parameter)
    {
        _source.print(parameter.getType());
        _source.print(" ");
        _source.print(parameter.getName());

        return;
    } // end of visit(SmcParameter)


	/**
	 * Emits C# code for passing the transition parameter to another method.
	 */
	private void passParameter(SmcParameter param)
	{
        String paramType=param.getType().trim();
        
        if ( paramType.startsWith( "ref " ) )
        {
        	_source.print( "ref ");                    	
        } else if (paramType.startsWith( "out " ) )
        {
        	_source.print( "out ");
        }
        _source.print(param.getName());
	}
    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data
//
} // end of class SmcCSharpGenerator

//
// CHANGE LOG
// $Log: SmcCSharpGenerator.java,v $
// Revision 1.12  2011/11/20 14:58:33  cwrapp
// Check in for SMC v. 6.1.0
//
// Revision 1.11  2009/12/17 19:51:43  cwrapp
// Testing complete.
//
// Revision 1.10  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.9  2009/11/25 15:09:45  cwrapp
// Corrected getStates.
//
// Revision 1.8  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.7  2009/10/06 15:31:59  kgreg99
// 1. Started implementation of feature request #2718920.
//     1.1 Added method boolean isStatic() to SmcAction class. It returns false now, but is handled in following language generators: C#, C++, java, php, VB. Instance identificator is not added in case it is set to true.
// 2. Resolved confusion in "emtyStateStack" keyword handling. This keyword was not handled in the same way in all the generators. I added method boolean isEmptyStateStack() to SmcAction class. This method is used instead of different string comparisons here and there. Also the generated method name is fixed, not to depend on name supplied in the input sm file.
//
// Revision 1.6  2009/10/05 13:54:45  kgreg99
// Feature request #2865719 implemented.
// Added method "passParameter" to SmcCSharpGenerator class. It shall be used to generate C# code if a transaction parameter shall be passed to another method. It preserves "ref" and "out" modifiers.
//
// Revision 1.5  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.4  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.3  2009/03/30 21:23:47  kgreg99
// 1. Patch for bug #2679204. Source code was compared to SmcJavaGenerator. At the end of function Visit(SmcGuard ... ) the condition to emit Entry() code was changed. Notice: there are other disimilarities in checking conditions in that function !
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
// Revision 1.12  2008/03/21 14:03:16  fperrad
// refactor : move from the main file Smc.java to each language generator the following data :
//  - the default file name suffix,
//  - the file name format for the generated SMC files
//
// Revision 1.11  2008/01/14 19:59:23  cwrapp
// Release 5.0.2 check-in.
//
// Revision 1.10  2007/02/21 13:54:15  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.9  2007/01/15 00:23:50  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.8  2006/09/16 15:04:28  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.7  2006/06/03 19:39:25  cwrapp
// Final v. 4.3.1 check in.
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
// Revision 1.5  2005/09/19 15:20:03  cwrapp
// Changes in release 4.2.2:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (C#) -csharp not generating finally block closing brace.
//
// Revision 1.4  2005/09/14 01:51:33  cwrapp
// Changes in release 4.2.0:
// New features:
//
// None.
//
// Fixed the following bugs:
//
// + (Java) -java broken due to an untested minor change.
//
// Revision 1.3  2005/08/26 15:21:34  cwrapp
// Final commit for release 4.2.0. See README.txt for more information.
//
// Revision 1.2  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the generate Context
// class' accessibility level in Java and C#.
//
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.2  2005/02/21 15:34:38  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.1  2005/02/21 15:10:36  charlesr
// Modified isLoopback() to new signature.
//
// Revision 1.0  2005/02/03 17:10:08  charlesr
// Initial revision
//
