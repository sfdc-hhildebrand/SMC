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
// $Id: SmcFSM.java,v 1.5 2010/02/15 18:05:43 fperrad Exp $
//
// CHANGE LOG
// (See the bottom of this file.)
//

package net.sf.smc.model;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The top-level element for a finite state machine model.
 * Contains the global state machine data:
 * <ul>
 *   <li>
 *     The FSM start state name.
 *   </li>
 *   <li>
 *     The optionsal {@literal %{, %} }raw source code which is
 *     copied verbatim to the generated file.
 *   </li>
 *   <li>
 *     The associated context class name.
 *   </li>
 *   <li>
 *     The optional header file containing the context class
 *     declaration.
 *   </li>
 *   <li>
 *     The optional list of included header files.
 *   </li>
 *   <li>
 *     The FSM package/namespace name.
 *   </li>
 *   <li>
 *     The list of package and/or class imports.
 *   </li>
 *   <li>
 *     The list of optional forward declarations.
 *   </li>
 *   <li>
 *     The list of state machine maps which containing the
 *     states.
 *   </li>
 * </ul>
 *
 * @author <a href="mailto:rapp@acm.org">Charles Rapp</a>
 */

public final class SmcFSM
    extends SmcElement
{
//---------------------------------------------------------------
// Member methods
//

    //-----------------------------------------------------------
    // Constructors.
    //

    /**
     * Creates a finite state machine of the given name. Data
     * members are set to default values.
     * @param name The finite state machine name.
     * @param targetFile The target filename with suffix.
     */
    public SmcFSM(final String name, final String targetFile)
    {
        // The abstract syntax tree always starts on line 1.
        super (name, 1);

        _startState = "";
        _source = "";
        _context = "";
        _sourceFileName = name;
        _targetFileName = targetFile;
        _fsmClassName = name + "Context";
        _header = "";
        _includeList = new ArrayList<String>();
        _package = null;
        _importList = new ArrayList<String>();
        _declareList = new ArrayList<String>();
        _accessLevel = "";
        _headerLine = -1;
        _maps = new ArrayList<SmcMap>();
    } // end of SmcFSM(String)

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
     * Returns the raw {@literal %{, %} }source code.
     * @return the raw {@literal %{, %} }source code.
     */
    public String getSource()
    {
        return (_source);
    } // end of getSource()

    /**
     * Returns the current .sm header line being processed.
     * @return the current .sm header line being processed.
     */
    public int getHeaderLine()
    {
        return (_headerLine);
    } // end of getHeaderLine()

    /**
     * Returns the start state name.
     * @return the start state name.
     */
    public String getStartState()
    {
        return (_startState);
    } // end of getStartState()

    /**
     * Returns the context class name.
     * @return the context class name.
     */
    public String getContext()
    {
        return (_context);
    } // end of getContext()

    /**
     * Returns the source file name.
     * @return the source file name.
     */
    public String getSourceFileName()
    {
        return (_sourceFileName);
    } // end of getSourceFileName()

    /**
     * Returns the target file name.
     * @return the target file name.
     */
    public String getTargetFileName()
    {
        return (_targetFileName);
    } // end of getTargetFileName()

    /**
     * Returns the fsm class name.
     * @return the fsm class name.
     */
    public String getFsmClassName()
    {
        return (_fsmClassName);
    } // end of getFsmClassName()

    /**
     * Returns the context class header file name.
     * @return the context class header file name.
     */
    public String getHeader()
    {
        return (_header);
    } // end of getHeader()

    /**
     * Returns the included header file list.
     * @return the included header file list.
     */
    public List<String> getIncludes()
    {
        return (_includeList);
    } // end of getIncludes()

    /**
     * Returns the package/namespace name.
     * @return the package/namespace name.
     */
    public String getPackage()
    {
        return (_package);
    } // end of getPackage()

    /**
     * Returns the imported package and/or class list.
     * @return the imported package and/or class list.
     */
    public List<String> getImports()
    {
        return (_importList);
    } // end of getImports()

    /**
     * Returns the number of imports.
     * @return the number of imports.
     */
    public int getImportCount()
    {
        return (_importList.size());
    } // end of getImportCount()

    /**
     * Returns the number of forward declarations.
     * @return the number of forward declarations.
     */
    public int getDeclareCount()
    {
        return (_declareList.size());
    } // end of getDeclareCount()

    /**
     * Returns the forward declarations list.
     * @return the forward declarations list.
     */
    public List<String> getDeclarations()
    {
        return (_declareList);
    } // end of getDeclarations()

    /**
     * Returns the generated FSM class access level. Used for
     * Java and C#.
     * @return the generated FSM class access level.
     */
    public String getAccessLevel()
    {
        return (_accessLevel);
    } // end of getAccessLevel()

    /**
     * Returns the named map. May return {@code null}.
     * @return the named map.
     */
    public SmcMap findMap(String name)
    {
        Iterator<SmcMap> mapIt;
        SmcMap map;
        SmcMap retval = null;

        for (mapIt = _maps.iterator(), retval = null;
             mapIt.hasNext() == true && retval == null;
            )
        {
            map = mapIt.next();
            if (map.getName().compareTo(name) == 0)
            {
                retval = map;
            }
        }

        return (retval);
    } // end of findMap(String)

    /**
     * Returns the map list.
     * @return the map list.
     */
    public List<SmcMap> getMaps()
    {
        return (_maps);
    } // end of getMaps()

    /**
     * Returns the list of all known transitions for all maps.
     * @return the list of all known transitions for all maps.
     */
    public List<SmcTransition> getTransitions()
    {
        Comparator<SmcTransition> comparator =
            new Comparator<SmcTransition>()
            {
                public int compare(SmcTransition o1,
                                   SmcTransition o2)
                {
                    return(o1.compareTo(o2));
                }
            };
        List<SmcTransition> retval =
            new ArrayList<SmcTransition>();

        for (SmcMap map: _maps)
        {
            // Merge the new transitions into the current set.
            retval =
                merge(map.getTransitions(), retval, comparator);
        }

        return (retval);
    } // end of getTransitions()

    public boolean hasEntryActions()
    {
        for (SmcMap map: _maps)
        {
            if (map.hasEntryActions() == true)
            {
                return true;
            }
        }
        return false;
    } // end of hasEntryActions()

    /**
     * Returns {@code true} if there is at least one state in the
     * state machine which has an exit action; {@code false}
     * otherwise.
     * @return {@code true} if there is at least one state in the
     * state machine which has an exit action; {@code false}
     * otherwise.
     */
    public boolean hasExitActions()
    {
        for (SmcMap map: _maps)
        {
            if (map.hasExitActions() == true)
            {
                return true;
            }
        }
        return false;
    } // end of hasExitActions()

    //
    // end of Get methods.
    //-----------------------------------------------------------

    //-----------------------------------------------------------
    // Set methods.
    //

    /**
     * Sets the raw source to be placed at the start of the
     * generated target code source file.
     * @param source the raw target source code.
     */
    public void setSource(String source)
    {
        _source = source;
        return;
    } // end of setSource(String)

    /**
     * Set the .sm source file line being processed.
     * @param lineNumber the .sm source file line.
     */
    public void setHeaderLine(int lineNumber)
    {
        if (_headerLine < 0)
        {
            _headerLine = lineNumber;
        }

        return;
    } // end of setHeaderLine(int)

    /**
     * Sets the FSM start state name.
     * @param state start state name.
     */
    public void setStartState(String state)
    {
        _startState = state;
        return;
    } // end of setStartState(String)

    /**
     * Set sthe context class name.
     * @param context class name.
     */
    public void setContext(String context)
    {
        _context = context;
        return;
    } // end of setContext(String)

    /**
     * Set the fsm class name and source file name.
     * @param fsmName The finite state machine's class name.
     */
    public void setFsmClassName(String fsmName)
    {
        _fsmClassName = fsmName;
        _targetFileName = fsmName;

        return;
    } // end of setFsmClassName(String)

    /**
     * Sets the context class header file name.
     * @param header header file name.
     */
    public void setHeader(String header)
    {
        char c = header.charAt(0);

        // If the header is not enclosed in quotes or <>, then
        // place quotes around the file now.
        if (c != '"' && c != '<')
        {
            _header = "\"" + header.trim() + "\"";
        }
        else
        {
            _header = header.trim();
        }

        _includeList.add(_header);

        return;
    } // end of setHeader(String)

    /**
     * Adds an include file name to the list.
     * @param includeFile an include file name.
     */
    public void addInclude(String includeFile)
    {
        char c = includeFile.charAt(0);
        String filename;

        // If the header is not enclosed in quotes or <>, then
        // place quotes around the file now.
        if (c != '"' && c != '<')
        {
            filename = "\"" + includeFile.trim() + "\"";
        }
        else
        {
            filename = includeFile.trim();
        }

        _includeList.add(filename);
        return;
    } // end of addInclude(String)

    /**
     * Sets the package/namespace name.
     * @param pkg package/namespace name.
     */
    public void setPackage(String pkg)
    {
        _package = pkg;
        return;
    } // end of setPackage(String)

    /**
     * Adds an import to the list.
     * @param name an imported name.
     */
    public void addImport(String name)
    {
        _importList.add(name);
        return;
    } // end of addImport(String)

    /**
     * Adds a forward declaration name.
     * @param name a forward declared name.
     */
    public void addDeclare(String name)
    {
        _declareList.add(name);
        return;
    } // end of addDeclare(String)

    /**
     * Sets the FSM class access level.
     * @param accessLevel a Java and or C# access level.
     */
    public void setAccessLevel(String accessLevel)
    {
        _accessLevel = accessLevel;
    } // end of setAccessLevel(String)

    /**
     * Adds a state machine map to the list.
     * @param map a finite state machine map.
     */
    public void addMap(SmcMap map)
    {
        _maps.add(map);
        return;
    } // end of addMap(SmcMap)

    //
    // end of Set methods.
    //-----------------------------------------------------------

    /**
     * Writes this state machine configuration to the given
     * stream.
     * @param stream write this state machine to this stream.
     */
    public void dump(PrintStream stream)
    {
        stream.print("Start State: ");
        stream.println(_startState);
        stream.print("     Source:");
        if (_source.length() == 0)
        {
            stream.println(" none.");
        }
        else
        {
            stream.println();
            stream.println(_source);
        }
        stream.print("    Context: ");
        stream.println(_context);

        for (String include: _includeList)
        {
            stream.print("     Include: ");
            stream.println(include);
        }

        stream.println("       Maps:");
        stream.println();

        for (SmcMap map: _maps)
        {
            stream.println(map);
        }

        return;
    } // end of dump(PrintStream)

//---------------------------------------------------------------
// Member data
//

    // The state map's initial state.
    private String _startState;

    // Raw source code appearing at the beginning of the state
    // map source file.
    private String _source;

    // This state map is associated with this class.
    private String _context;

    // The FSM is readen to this file.
    private String _sourceFileName;

    // The FSM is written to this file.
    private String _targetFileName;

    // This map is implemented in the class with given name
    private String _fsmClassName;

    // Where the associated class is defined.
    private String _header;

    // For C++ only. List of include files. Will be output to
    // the .cpp file in the same order as they appear in the
    // .sm file.
    private List<String> _includeList;

    // This code is placed in this package/namespace.
    private String _package;

    // Place names of imports in this list.
    private List<String> _importList;

    // Place forward declarations in this list.
    private List<String> _declareList;

    // The context class access level. Empty string by default.
    private String _accessLevel;

    // The line where %start, etc. should appear.
    // Used in error messages.
    private int _headerLine;

    // The state maps.
    private List<SmcMap> _maps;
} // end of class SmcFSM

//
// CHANGE LOG
// $Log: SmcFSM.java,v $
// Revision 1.5  2010/02/15 18:05:43  fperrad
// fix 2950619 : make distinction between source filename (*.sm) and target filename.
//
// Revision 1.4  2009/11/25 22:30:19  cwrapp
// Fixed problem between %fsmclass and sm file names.
//
// Revision 1.3  2009/11/24 20:42:39  cwrapp
// v. 6.0.1 update
//
// Revision 1.2  2009/09/12 21:44:49  kgreg99
// Implemented feature req. #2718941 - user defined generated class name.
// A new statement was added to the syntax: %fsmclass class_name
// It is optional. If not used, generated class is called as before "XxxContext" where Xxx is context class name as entered via %class statement.
// If used, generated class is called asrequested.
// Following language generators are touched:
// c, c++, java, c#, objc, lua, groovy, scala, tcl, VB
// This feature is not tested yet !
// Maybe it will be necessary to modify also the output file name.
//
// Revision 1.1  2009/03/01 18:20:42  cwrapp
// Preliminary v. 6.0.0 commit.
//
// Revision 1.7  2008/02/08 08:46:02  fperrad
// C : optimize footprint when no Entry action or no Exit action
//
// Revision 1.6  2007/02/21 13:54:45  cwrapp
// Moved Java code to release 1.5.0
//
// Revision 1.5  2007/01/15 00:23:51  cwrapp
// Release 4.4.0 initial commit.
//
// Revision 1.4  2006/09/16 15:04:29  cwrapp
// Initial v. 4.3.3 check-in.
//
// Revision 1.3  2005/11/07 19:34:54  cwrapp
// Changes in release 4.3.0:
// New features:
//
// + Added -reflect option for Java, C#, VB.Net and Tcl code
//   generation. When used, allows applications to query a state
//   about its supported transitions. Returns a list of
//   transition names. This feature is useful to GUI developers
//   who want to enable/disable features based on the current
//   state. See Programmer's Manual section 11: On Reflection
//   for more information.
//
// + Updated LICENSE.txt with a missing final paragraph which allows
//   MPL 1.1 covered code to work with the GNU GPL.
//
// + Added a Maven plug-in and an ant task to a new tools
//   directory.
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
//   certainly *not* thread safe. Multi-threaded C/C++
//   applications are required to synchronize access to the FSM
//   to allow for correct performance.
//
// + (Java) The generated getState() method is now public.
//
// Revision 1.2  2005/06/30 10:44:23  cwrapp
// Added %access keyword which allows developers to set the
// generate Context class' accessibility level in Java and C#.
//
// Revision 1.1  2005/05/28 19:28:42  cwrapp
// Moved to visitor pattern.
//
// Revision 1.6  2005/02/21 15:35:11  charlesr
// Added Francois Perrad to Contributors section for Python work.
//
// Revision 1.5  2005/02/03 16:44:40  charlesr
// In implementing the Visitor pattern, the generateCode()
// methods have been moved to the appropriate Visitor
// subclasses (e.g. SmcJavaGenerator). This class is now
// extends SmcElement.
//
// Also changed this class name from SmcParseTree to SmcFSM.
//
// Revision 1.4  2004/10/30 16:06:54  charlesr
// Added Graphviz DOT file generation.
//
// Revision 1.3  2004/10/02 19:53:57  charlesr
// Removed "using namespace std" from the generated .cpp file.
// References to std namespace are fully-qualified with "std::".
// Also replaced all string concatenation from print statements.
//
// Revision 1.2  2004/09/06 16:40:49  charlesr
// Added C# support.
//
// Revision 1.1  2004/05/31 13:55:51  charlesr
// Added support for VB.net code generation.
//
// Revision 1.0  2003/12/14 21:04:51  charlesr
// Initial revision
//
