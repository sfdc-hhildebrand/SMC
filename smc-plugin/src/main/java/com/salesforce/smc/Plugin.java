/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.smc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import net.sf.smc.Smc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @author hhildebrand
 * 
 *         Goal which touches a timestamp file.
 * 
 * @goal generate
 * 
 * @phase generate-sources
 */
public class Plugin extends AbstractMojo {

    /**
     * DebugLevel. 0, 1: Adds debug output messages to the generated code. 0
     * produces output messages which signal when the FSM has exited a state,
     * entered a state, entered and exited a transition. 1 includes the 0 output
     * and addition output for entering and exiting state Exit and Entry
     * actions.
     * 
     * @parameter
     */
    private int          debugLevel      = -1;

    /**
     * Generated documentation directory, relative to the project based
     * directory. This is where the HTML table and DOT graph will be generated.
     * Defaults to the targetDirectory.
     * 
     * @parameter
     */
    private String       docDirectory;

    /**
     * FSM verbose output
     * 
     * @parameter
     */
    private boolean      fsmVerbose      = false;

    /**
     * Generic collections. May be used only with target languages csharp, java
     * or vb and reflection. Causes SMC to use generic collections for
     * reflection.
     * 
     * @parameter
     */
    private boolean      generic         = true;

    /**
     * Produce DOT graph output.
     * 
     * @parameter
     */
    private boolean      graph           = false;

    /**
     * Graph level. Specifies how much detail to place into the DOT file. Level
     * 0 is the least detail and 2 is the greatest.
     * 
     * @parameter
     */
    private int          graphLevel      = -1;

    /**
     * Project instance.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Reflection. May be used only with target languages csharp, groovy, java,
     * lua, perl, php, python, ruby, scala, tcl and vb. Causes SMC to generate a
     * getTransitions method, allowing applications to query the available
     * transitions for the current state.
     * 
     * @parameter
     */
    private boolean      reflection      = false;

    /**
     * Serialization. Generate unique integer IDs for each state. These IDs can
     * be used when persisting an FSM.
     * 
     * @parameter
     */
    private boolean      serial          = true;

    /**
     * State machine files source directory, relative to the project root.
     * 
     * @parameter
     */
    private String       smDirectory     = "src/main/sm";

    /**
     * May be used only with the java, groovy, scala, vb and csharp target
     * languages. Causes SMC to:
     * <ul>
     * <li>Java: add the synchronized keyword to the transition method
     * declarations.</li>
     * <li>Groovy: add the synchronized keyword to the transition method
     * declarations.</li>
     * <li>Scala: add the synchronized keyword to the transition method
     * declarations.</li>
     * <li>VB.net: encapsulate the transition method's body in a SyncLock Me,
     * End SyncLock block.</li>
     * <li>C#: encapsulate the transition method's body in a lock(this) {...}
     * block.</li>
     * </ul>
     * 
     * @parameter
     */
    private boolean      sync            = false;

    /**
     * Produce HTML table output.
     * 
     * @parameter
     */
    private boolean      table           = false;

    /**
     * Target language
     * 
     * @parameter
     */
    private String       target          = "java";

    /**
     * Generated source directory, relative to the project base directory
     * 
     * @parameter
     */
    private String       targetDirectory = "target/generated-sources/sm";

    /**
     * Verbose output.
     * 
     * @parameter
     */
    private boolean      verbose         = false;

    @Override
    public void execute() throws MojoExecutionException {
        if (docDirectory == null) {
            docDirectory = targetDirectory;
        }

        ArrayList<String> commonArgs = new ArrayList<String>();

        commonArgs.add("-return");
        File targetDir = new File(project.getBasedir(), targetDirectory);
        targetDir.mkdirs();

        File srcDir = new File(project.getBasedir(), smDirectory);

        project.addCompileSourceRoot(srcDir.getAbsolutePath());
        project.addCompileSourceRoot(targetDir.getAbsolutePath());

        ArrayList<String> sources = new ArrayList<String>();
        for (File source : srcDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".sm");
            }
        })) {
            sources.add(source.getAbsolutePath());
        }

        ArrayList<String> args = new ArrayList<String>(commonArgs);

        switch (debugLevel) {
            case 0:
                args.add("-g0");
                break;
            case 1:
                args.add("-g1");
                break;
            default:
        }

        if (verbose) {
            args.add("-verbose");
        }

        if (fsmVerbose) {
            args.add("-vverbose");
        }
        if (sync) {
            args.add("-sync");
        }

        if (serial) {
            args.add("-serial");
        }

        if (reflection) {
            args.add("-reflection");
            if (generic) {
                args.add("-generic");
            }
        }

        args.add("-d");
        args.add(targetDir.getAbsolutePath());
        args.add("-" + target);
        args.addAll(sources);

        // generate FSM source
        Smc.main(args.toArray(new String[0]));

        File docDir = new File(project.getBasedir(), docDirectory);
        docDir.mkdirs();

        commonArgs.add("-d");
        commonArgs.add(docDir.getAbsolutePath());

        if (graph) {
            args = new ArrayList<String>(commonArgs);
            args.add("-graph");
            switch (graphLevel) {
                case 0:
                    args.add("-gLevel");
                    args.add("0");
                    break;
                case 1:
                    args.add("-gLevel");
                    args.add("1");
                    break;
                default:
            }
            args.addAll(sources);

            // Generate graphs
            Smc.main(args.toArray(new String[0]));
        }

        if (table) {
            args = new ArrayList<String>(commonArgs);
            args.add("-table");
            args.addAll(sources);

            // Generate graphs
            Smc.main(args.toArray(new String[0]));
        }
    }

    /**
     * @param debugLevel
     *            the debugLevel to set
     */
    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    /**
     * @param docDirectory
     *            the docDirectory to set
     */
    public void setDocDirectory(String docDirectory) {
        this.docDirectory = docDirectory;
    }

    /**
     * @param fsmVerbose
     *            the fsmVerbose to set
     */
    public void setFsmVerbose(boolean fsmVerbose) {
        this.fsmVerbose = fsmVerbose;
    }

    /**
     * @param generic
     *            the generic to set
     */
    public void setGeneric(boolean generic) {
        this.generic = generic;
    }

    /**
     * @param graph
     *            the graph to set
     */
    public void setGraph(boolean graph) {
        this.graph = graph;
    }

    /**
     * @param graphLevel
     *            the graphLevel to set
     */
    public void setGraphLevel(int graphLevel) {
        this.graphLevel = graphLevel;
    }

    /**
     * @param project
     *            the project to set
     */
    public void setProject(MavenProject project) {
        this.project = project;
    }

    /**
     * @param reflection
     *            the reflection to set
     */
    public void setReflection(boolean reflection) {
        this.reflection = reflection;
    }

    /**
     * @param serial
     *            the serial to set
     */
    public void setSerial(boolean serial) {
        this.serial = serial;
    }

    /**
     * @param smDirectory
     *            the smDirectory to set
     */
    public void setSmDirectory(String smDirectory) {
        this.smDirectory = smDirectory;
    }

    /**
     * @param sync
     *            the sync to set
     */
    public void setSync(boolean sync) {
        this.sync = sync;
    }

    /**
     * @param table
     *            the table to set
     */
    public void setTable(boolean table) {
        this.table = table;
    }

    /**
     * @param target
     *            the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * @param targetDirectory
     *            the targetDirectory to set
     */
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    /**
     * @param verbose
     *            the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
