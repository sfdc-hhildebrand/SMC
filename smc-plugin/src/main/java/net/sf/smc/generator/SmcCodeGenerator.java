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
// $Id: SmcCodeGenerator.java,v 1.6 2010/02/15 18:05:43 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.text.MessageFormat;
import net.sf.smc.model.SmcElement;
import net.sf.smc.model.SmcElement.TransType;
import net.sf.smc.model.SmcGuard;
import net.sf.smc.model.SmcVisitor;

/**
 * Base class for all target language code generators. The
 * syntax tree visitation methods of the
 * {@link net.sf.smc.model.SmcVisitor} super class are left to
 * this class' subclasses to define.
 *
 * @see SmcElement
 * @see SmcVisitor
 * @see SmcOptions
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public abstract class SmcCodeGenerator
    extends SmcVisitor
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Constructs the target code generator for the given
     * parameters. All subclass constructors receive the same
     * arguments even though not all arguments apply to every
     * concrete code generator.
     * @param options The target code generator options.
     * @param suffix the target source file name suffix.
     */
    protected SmcCodeGenerator(final SmcOptions options,
                               final String suffix)
    {
        _srcfileBase = options.srcfileBase();
        _targetfileBase = options.targetfileBase();
        _srcDirectory = options.srcDirectory();
        _headerDirectory = options.headerDirectory();
        _castType = options.castType();
        _graphLevel = options.graphLevel();
        _serialFlag = options.serialFlag();
        _debugLevel = options.debugLevel();
        _noExceptionFlag = options.noExceptionFlag();
        _noCatchFlag = options.noCatchFlag();
        _noStreamsFlag = options.noStreamsFlag();
        _reflectFlag = options.reflectFlag();
        _syncFlag = options.syncFlag();
        _genericFlag = options.genericFlag();
        _accessLevel = options.accessLevel();
        _suffix = suffix;
        _source = null;
        _indent = "";
        _guardCount = 0;
        _guardIndex = 0;
    } // end of SmcCodeGenerator(SmcOptions)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // SmcVisitor Abstract Method Impelementation.
    //

    // Left undefined for the subclasses.

    //
    // end of SmcVisitor Abstract Method Impelementation.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the source file name generated from the
     * destination directory, base name and suffix using
     * the source name format.
     * @param path The destination directory.
     * @param packageName
     * @param basename The file's basename sans suffix.
     * @param suffix Append this suffix to the file.
     */
    public String sourceFile(final String path,
                             final String packageName,
                             final String basename, final String suffix)
    {
        MessageFormat formatter =
            new MessageFormat(SOURCE_PATH_FORMAT);
        Object[] args = new Object[3];

        args[0] = appendPackageName(path, packageName);
        args[1] = basename;
        if (suffix == null)
        {
            args[2] = _suffix;
        }
        else
        {
            args[2] = suffix;
        }

        return (formatter.format(args));
    } // end of sourceFile(String, String, String)

    protected Object appendPackageName(String path, String packageName) {
        return path;
    }

    /**
     * Returns {@code true} if this transition is an
     * <i>internal</i> loopback or a push transition and
     * {@code false} otherwise. If true, then do not perform the
     * the state exit and entry actions.
     * @param transType the transition type.
     * @param endState entering this state.
     * @return {@code true} if this transition is an internal
     * loopback or push transition and {@code false} otherwise.
     */
    protected boolean isLoopback(TransType transType,
                                 String endState)
    {
        return (
            (transType == TransType.TRANS_SET ||
             transType == TransType.TRANS_PUSH) &&
            endState.equals(SmcElement.NIL_STATE) == true);
    } // end of isLoopback(int transType, String)

    /**
     * Returns {@code true} if each of the transition guards uses
     * the nil end state.
     * @param guards check if all this transitions use the nil
     * end state.
     * @return {@code true} if each of the transition guards uses
     * the nil end state.
     */
    protected boolean allNilEndStates(List<SmcGuard> guards)
    {
        Iterator<SmcGuard> git;
        SmcGuard guard;
        boolean retcode = true;

        for (git = guards.iterator();
             git.hasNext() == true && retcode == true;
            )
        {
            guard = git.next();
            retcode =
                (guard.getTransType() == TransType.TRANS_SET &&
                 (guard.getEndState()).equals("nil") == true);
        }

        return (retcode);
    } // end of allNilEndStates(List<SmcGuard>)

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the source code output destination.
     * @param source the generated source code output stream.
     */
    public void setSource(PrintStream source)
    {
        _source = source;
        return;
    } // end of setSource(PrintStream)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Place a backslash escape character in front of backslashes
     * and doublequotes.
     * @param s Escape this string.
     * @return the backslash escaped string.
     */
    public static String escape(String s)
    {
        String retval;

        if (s.indexOf('\\') < 0 && s.indexOf('"') < 0)
        {
            retval = s;
        }
        else
        {
            StringBuffer buffer =
                new StringBuffer(s.length() * 2);
            int index;
            int length = s.length();
            char c;

            for (index = 0; index < length; ++index)
            {
                c = s.charAt(index);
                if (c == '\\' || c == '"')
                {
                    buffer.append('\\');
                }

                buffer.append(c);
            }

            retval = buffer.toString();
        }

        return (retval);
    } // end of escape(String)

    // Scope the state name. If the state is unscoped, then
    // return "<mapName>.<stateName>". If the state named
    // contains the scope string "::", replace that with a ".".
    protected String scopeStateName(String stateName,
                                    String mapName)
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
    } // end of scopeStateName(String, String)

//---------------------------------------------------------------
// Member data
//

    /**
     * Emit the target source code to this output stream.
     */
    protected PrintStream _source;

    /**
     * The .sm file's base name.
     */
    protected final String _srcfileBase;

    /**
     * The target file's base name.
     */
    protected final String _targetfileBase;

    /**
     * Write the target source file to this directory.
     */
    protected final String _srcDirectory;

    /**
     * Place the generated header file in this directory.
     */
    protected final String _headerDirectory;

    /**
     * Use this cast type (C++ only).
     */
    protected final String _castType;

    /**
     * Generate this much detail in the graph (-graph only).
     */
    protected final int _graphLevel;

    /**
     * Output this indent before generating a line of code.
     */
    protected String _indent;

    // This information is common between the transition and
    // guard visitor methods.
    /**
     * The total number of guards to be generated at this time.
     */
    protected int _guardCount;

    /**
     * The guard currently being generated.
     */
    protected int _guardIndex;

    /**
     * This flag is true when serialization is to be generated.
     */
    protected final boolean _serialFlag;

    /**
     * This flag is true when debug output is to be generated.
     */
    protected final int _debugLevel;

    /**
     * This flag is true when exceptions are not be thrown.
     */
    protected final boolean _noExceptionFlag;

    /**
     * This flag is true when exceptions are not caught.
     */
    protected final boolean _noCatchFlag;

    /**
     * This flag is true when I/O streams should not be used.
     */
    protected final boolean _noStreamsFlag;

    /**
     * This flag is true when reflection is supported.
     */
    protected final boolean _reflectFlag;

    /**
     * This flag is true when synchronization code is to be
     * generated.
     */
    protected final boolean _syncFlag;

    /**
     * This flag is true when reflection is to use a
     * generic transition map. Used with -java and -reflect only.
     */
    protected final boolean _genericFlag;

    /**
     * Used this access keyword for the generated classes.
     */
    protected final String _accessLevel;

    //-----------------------------------------------------------
    // Statics.
    //

    // Append this suffix to the end of the output file.
    private static String _suffix;

    //-----------------------------------------------------------
    // Constants.
    //

    // Debug output detail level.

    /**
     * No debug output.
     */
    public static final int NO_DEBUG_OUTPUT = -1;

    /**
     * Output states and transitions.
     */
    public static final int DEBUG_LEVEL_0 = 0;

    /**
     * Output states, transitions and all transition, entry and
     * exit actions.
     */
    public static final int DEBUG_LEVEL_1 = 1;

    // GraphViz detail level.

    /**
     * No graphing is done.
     */
    public static final int NO_GRAPH_LEVEL = -1;

    /**
     * Provide state and transition names only.
     */
    public static final int GRAPH_LEVEL_0 = 0;

    /**
     * Provide state and transition names plus transition guards
     * and actions.
     */
    public static final int GRAPH_LEVEL_1 = 1;

    /**
     * Provides state names, entry and exit actions, transition
     * name and arguments, guards, actions and their action
     * parameters and pop transition arguments.
     */
    public static final int GRAPH_LEVEL_2 = 2;

    // The source file name path format.
    private static final String SOURCE_PATH_FORMAT =
        "{0}{1}.{2}";
} // end of class SmcCodeGenerator

//
// CHANGE LOG
// $Log: SmcCodeGenerator.java,v $
// Revision 1.6  2010/02/15 18:05:43  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.5  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.4  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.3  2009/09/05 15:39:20  cwrapp
// Checking in fixes for 1944542, 1983929, 2731415, 2803547 and feature 2797126.
//
// Revision 1.2  2009/03/16 19:45:26  cwrapp
// Corrected isLoopback.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
//
