package org.iseclab.drammer.code;


import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessKiller {

    private static String TAG = ProcessKiller.class.getSimpleName();

    private class Process {
        int pid;
        String name;

        public Process(int pid, String name){
            this.pid = pid;
            this.name = name;
        }
    }

    public boolean isRunning(String name){
        List<Process> processes = getRunningProcesses();
        for(Process process: processes){
            if((process.name != null) && (process.name.contains(name))){
                return true;
            }
        }
        return false;
    }


    public void killProcesses(String name, int signal){
        List<Process> processes = getRunningProcesses();
        for(Process process: processes){
            try {
                if ((process.name == null) || (!process.name.contains(name))) {
                    continue;
                }
                Log.d(TAG, "Killing " + process.name);
                Runtime.getRuntime().exec("kill -" + signal + " " + process.pid);
            } catch (IOException e) {
                Log.e(TAG, "Failed to kill process " + process.name + " (" + process.pid + ")" + ": " + e.getMessage());
            } catch (Exception igored) {}
        }
    }

    public List<Process> getRunningProcesses() {
        List<Process> processes = new ArrayList<>();
        File[] files = new File("/proc").listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                int pid;
                String name = "";
                try {
                    pid = Integer.parseInt(file.getName());
                } catch (NumberFormatException e) {
                    continue;
                }

                BufferedReader reader = null;
                StringBuilder output = null;

                try {
                    reader = new BufferedReader(new FileReader(String.format("/proc/%d/stat", pid)));
                    output = new StringBuilder();
                    for (String line = reader.readLine(), newLine = ""; line != null; line = reader.readLine()) {
                        output.append(newLine).append(line);
                        newLine = "\n";
                    }
                    reader.close();
                    String content = output.toString().trim();
                    name = content.split("\\s+")[1].replace("(", "").replace(")", "");;
                } catch (IOException ignored) {
                } finally {
                    if(reader != null){
                        try {
                            reader.close();
                        } catch (IOException ignored){}
                    }
                }

                if(!name.isEmpty()) {
                    processes.add(new Process(pid, name));
                    continue;
                }

                try {
                    reader = new BufferedReader(new FileReader(String.format("/proc/%d/cmdline", pid)));
                    output = new StringBuilder();
                    for (String line = reader.readLine(), newLine = ""; line != null; line = reader.readLine()) {
                        output.append(newLine).append(line);
                        newLine = "\n";
                    }
                    name = output.toString().trim();
                    reader.close();
                } catch (IOException ignored) {
                } finally {
                    if(reader != null){
                        try {
                            reader.close();
                        } catch (IOException ignored){}
                    }
                }

                processes.add(new Process(pid,name));
            }
        }
        return processes;
    }

}
