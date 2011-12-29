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

import static junit.framework.Assert.assertTrue;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * 
 * @author hhildebrand
 * 
 */
public class TestPlugin {
    @Test
    public void testGenerate() throws Exception {
        File tempDir = File.createTempFile("smc", "generated", new File("."));
        try {
            MavenProject project = mock(MavenProject.class);
            when(project.getBasedir()).thenReturn(tempDir);
            tempDir.delete();
            tempDir.deleteOnExit();
            String docDirectory = "target/classes/generated-sources/sm";
            String targetDirectory = docDirectory;
            Plugin plugin = new Plugin();

            plugin.setDocDirectory(docDirectory);
            plugin.setDebugLevel(2);
            plugin.setFsmVerbose(false);
            plugin.setGeneric(true);
            plugin.setGraph(true);
            plugin.setGraphLevel(2);
            plugin.setProject(project);
            plugin.setReflection(true);
            plugin.setSerial(true);
            plugin.setSmDirectory("../src/test/resources/sm");
            plugin.setSync(true);
            plugin.setTable(true);
            plugin.setTarget("java");
            plugin.setTargetDirectory(targetDirectory);
            plugin.setVerbose(true);

            String packageName = "smc_ex5";
            File targetDir = new File(tempDir, targetDirectory);
            File packageDir = new File(targetDir, packageName);
            File docDir = new File(tempDir, docDirectory);

            plugin.execute();
            assertTrue("TaskFSM DOT file not generated",
                       new File(docDir, "TaskFSM.dot").exists());
            assertTrue("TaskFSM HTML table not generated",
                       new File(docDir, "TaskFSM.html").exists());
            assertTrue("TaskFSM.java not generated",
                       new File(packageDir, "TaskFSM.java").exists());

            assertTrue("TaskManagerFSM DOT file not generated",
                       new File(docDir, "TaskManagerFSM.dot").exists());
            assertTrue("TaskManagerFSM HTML table not generated",
                       new File(docDir, "TaskManagerFSM.html").exists());
            assertTrue("TaskManagerFSM.java not generated",
                       new File(packageDir, "TaskManagerFSM.java").exists());
        } finally {
            deleteDirectory(tempDir);
        }
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}
