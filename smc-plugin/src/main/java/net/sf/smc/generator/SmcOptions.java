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
// Copyright (C) 2009. Charles W. Rapp.
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
// $Id: SmcOptions.java,v 1.2 2010/02/15 18:05:43 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.generator;

/**
 * This passive, immutable class stores the SMC generator options
 * and an instance is passed to the generator constructors.
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcOptions
{
//---------------------------------------------------------------
// Member methods.
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Stores the target code generator options.
     * @param srcfileBase basename of source file (*.sm).
     * @param targetfileBase write the emitted code to this target
     * source file name sans the suffix.
     * @param srcDirectory place the target source file in this
     * directory.
     * @param headerDirectory place the target header file in
     * this directory. Ignored if there is no generated header
     * file.
     * @param castType use this type cast (C++ code generation
     * only).
     * @param graphLevel amount of detail in the generated
     * GraphViz graph (graph code generation only).
     * @param serialFlag if {@code true}, generate unique
     * identifiers for persisting the FSM.
     * @param debugLevel if &ge; zero add debug output messages
     * to code.
     * @param noExceptionFlag if {@code true} then use asserts
     * rather than exceptions (C++ only).
     * @param noCatchFlag if {@code true} then do <i>not</i>
     * generate try/catch/rethrow code.
     * @param noStreamsFlag if {@code true} then use TRACE macro
     * for debug output.
     * @param reflectFlag if {@code true} then generate
     * reflection code.
     * @param syncFlag if {@code true} then generate
     * synchronization code.
     * @param genericFlag if {@code true} then use generic
     * collections.
     * @param accessLevel use this access keyword for the
     * generated classes.
     */
    public SmcOptions(final String srcfileBase,
                      final String targetfileBase,
                      final String srcDirectory,
                      final String headerDirectory,
                      final String castType,
                      final int graphLevel,
                      final boolean serialFlag,
                      final int debugLevel,
                      final boolean noExceptionFlag,
                      final boolean noCatchFlag,
                      final boolean noStreamsFlag,
                      final boolean reflectFlag,
                      final boolean syncFlag,
                      final boolean genericFlag,
                      final String accessLevel)
    {
        _srcfileBase = srcfileBase;
        _targetfileBase = targetfileBase;
        _headerDirectory = headerDirectory;
        _castType = castType;
        _graphLevel = graphLevel;
        _srcDirectory = srcDirectory;
        _serialFlag = serialFlag;
        _debugLevel = debugLevel;
        _noExceptionFlag = noExceptionFlag;
        _noCatchFlag = noCatchFlag;
        _noStreamsFlag = noStreamsFlag;
        _reflectFlag = reflectFlag;
        _syncFlag = syncFlag;
        _genericFlag = genericFlag;
        _accessLevel = accessLevel;
    } // end f SmcOptions(...)

    //
    // end of Constructors.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Get methods.
    //

    /**
     * Returns the source file name's base.
     * @return the source file name's base.
     */
    public String srcfileBase()
    {
        return (_srcfileBase);
    } // end of srcfileBase()

    /**
     * Returns the target source file name's base.
     * @return the target source file name's base.
     */
    public String targetfileBase()
    {
        return (_targetfileBase);
    } // end of targetfileBase()

    /**
     * Returns the target source file's directory.
     * @return the target source file's directory.
     */
    public String srcDirectory()
    {
        return (_srcDirectory);
    } // end of srcDirectory()

    /**
     * Returns the target header file's directory.
     * @return the target header file's directory.
     */
    public String headerDirectory()
    {
        return (_headerDirectory);
    } // end of headerDirectory()

    /**
     * Returns the C++ cast type.
     * @return the C++ cast type.
     */
    public String castType()
    {
        return (_castType);
    } // end of castType()

    /**
     * Returns the GraphViz graph detail level.
     * @return the GraphViz graph detail level.
     */
    public int graphLevel()
    {
        return (_graphLevel);
    } // end of graphLevel()

    /**
     * Returns the serialization flag.
     * @return the serialization flag.
     */
    public boolean serialFlag()
    {
        return (_serialFlag);
    } // end of serialFlag()

    /**
     * Returns the debug output level.
     * @return the debug output level.
     */
    public int debugLevel()
    {
        return (_debugLevel);
    } // end of debugLevel()

    /**
     * Returns the "no exception" flag.
     * @return the "no exception" flag.
     */
    public boolean noExceptionFlag()
    {
        return (_noExceptionFlag);
    } // end of noExceptionFlag()

    /**
     * Returns the "no catch" flag.
     * @return the "no catch" flag.
     */
    public boolean noCatchFlag()
    {
        return (_noCatchFlag);
    } // end of noCatchFlag()

    /**
     * Returns the "no streams" flag.
     * @return the "no streams" flag.
     */
    public boolean noStreamsFlag()
    {
        return (_noStreamsFlag);
    } // end of noStreamsFlag()

    /**
     * Returns the reflection flag.
     * @return the reflection flag.
     */
    public boolean reflectFlag()
    {
        return (_reflectFlag);
    } // end of reflectFlag()

    /**
     * Returns the synchronization flag.
     * @return the synchronization flag.
     */
    public boolean syncFlag()
    {
        return (_syncFlag);
    } // end of syncFlag()

    /**
     * Returns the generic reflection flag.
     * @return the generic reflection flag.
     */
    public boolean genericFlag()
    {
        return (_genericFlag);
    } // end of genericFlag()

    /**
     * Returns the generated class access level.
     * @return the generated class access level.
     */
    public String accessLevel()
    {
        return (_accessLevel);
    } // end of accessLevel()

    //
    // end of Get methods.
    //-----------------------------------------------------------

//---------------------------------------------------------------
// Member data.
//

    // The .sm file's base name.
    private final String _srcfileBase;

    // The target file's base name.
    private final String _targetfileBase;

    // Write the target source file to this directory.
    private final String _srcDirectory;

    // Place the generated header file in this directory.
    private final String _headerDirectory;

    // Use this cast type (C++ only).
    private final String _castType;

    // Generate this much detail in the graph (-graph only).
    private final int _graphLevel;

    // This flag is true when serialization is to be generated.
    private final boolean _serialFlag;

    // Generate this much detail in the debug output.
    private final int _debugLevel;

    // This flag is true when exceptions are not be thrown.
    private final boolean _noExceptionFlag;

    // This flag is true when exceptions are not caught.
    private final boolean _noCatchFlag;

    // This flag is true when I/O streams should not be used.
    private final boolean _noStreamsFlag;

    // This flag is true when reflection is supported.
    private final boolean _reflectFlag;

    // This flag is true when synchronization code is to be
    // generated.
    private final boolean _syncFlag;

    // This flag is true when reflection is to use a
    // generic transition map. Used with -java and -reflect only.
    private final boolean _genericFlag;

    // Used this access keyword for the generated classes.
    private final String _accessLevel;
} // end of class SmcOptions

//
// CHANGE LOG
// $Log: SmcOptions.java,v $
// Revision 1.2  2010/02/15 18:05:43  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.1  2009/11/24 20:46:50  cwrapp
// Initial check in.
//
