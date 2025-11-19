import java.io.File;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String curr_dir=System.getProperty("user.dir");
        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            String[] commands = splitthestring(command);

            //exit condition
            if (command.equals("exit 0") || command.equals("exit")) break;

            //type command
            else if (commands[0].equals("type")) {
                String a = type_and_path_handling(commands[1]);
                System.out.println(a);
            }
            //echo command
            else if (commands[0].equals("echo")) {
                for (int i = 1; i < commands.length; i++) {
                    System.out.print(commands[i] + " ");
                }
                System.out.println();
            }
            else if (commands[0].equals("cd")) {

                // 1. Handle "cd" with no arguments
                if (commands.length < 2) {
                    System.out.println("cd: missing argument");
                    continue; // Skips to the next loop
                }

                String target_dir = commands[1];

                // 2. Handle the "~" (home) directory shortcut
                if (target_dir.equals("~")) {
                    target_dir = System.getenv("HOME");
                }

                // 3. THIS IS THE FIX: Handle absolute vs. relative paths
                File dir = new File(target_dir);

                // If the path is NOT absolute, then make it
                // relative to our current directory
                if (!dir.isAbsolute()) {
                    dir = new File(curr_dir, target_dir);
                }
                // 4. Check and update the directory
                if (dir.exists() && dir.isDirectory()) {
                    // MUST use getCanonicalPath() to resolve ".." and "."
                    curr_dir = dir.getCanonicalPath();
                } else {
                    System.out.println("cd: " + target_dir + ": No such file or directory");
                }
            }
            //pwd command
            else if (command.equals("pwd")) {
                System.out.println(curr_dir);
            }
            // external programs (search PATH and run)
            else {
                String executablePath = findExecutable(commands[0]);
                if (executablePath != null) {
                    runExternalProgram(executablePath, commands,curr_dir);
                } else {
                    System.out.println(commands[0] + ": command not found");
                }
            }
        }
    }

    public static String[] splitthestring(String command) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);

            if (ch == '\'') {
                inQuotes = !inQuotes; // toggle quoting
            }
            else if (Character.isWhitespace(ch) && !inQuotes) {
                // whitespace outside quotes splits tokens
                if (current.length() > 0) {
                    list.add(current.toString());
                    current.setLength(0);
                }
            }
            else {
                // normal character → append to current token
                current.append(ch);
            }
        }

        // Add last token if exists
        if (current.length() > 0) {
            list.add(current.toString());
        }

        return list.toArray(new String[0]);
    }


    public static String type_and_path_handling(String command) {
        String[] cmd = {"exit", "echo", "type","pwd","cd"};
        String pathEnv = System.getenv("PATH");
        String[] directory = pathEnv.split(File.pathSeparator, -1);
        for (String i : cmd) {
            if (Objects.equals(i, command)) {
                return i + " is a shell builtin";
            }
        }
        for (int j = 0; j < directory.length; j++) {
            if (directory[j].isEmpty()) directory[j] = ".";
            File file = new File(directory[j], command);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return command + " is " + file.getAbsolutePath();
            }
        }
        return command + ": not found";
    }

    // find the absolute path to an executable (returns null if not found)
    public static String findExecutable(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        String[] dirs = pathEnv.split(File.pathSeparator, -1);

        for (String dir : dirs) {
            if (dir.isEmpty()) dir = ".";
            File file = new File(dir, command);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static void runExternalProgram(String executablePath, String[] args,String currentDirectory) {
        try {
            File execFile = new File(executablePath);
            String parentDir = execFile.getParent();

            // what the user typed (program name)
            String shortName = args[0];

            // Build command array where argv[0] is the short name (user typed)
            List<String> cmdList = new ArrayList<>();
            cmdList.add(shortName);
            for (int i = 1; i < args.length; i++) {
                cmdList.add(args[i]);
            }

            ProcessBuilder pb = new ProcessBuilder(cmdList);

            //This line explicitly tells the ProcessBuilder: "Hey, before you start this new program (ls),
            // set its starting directory to this path."
            pb.directory(new File(currentDirectory));
            // Prepend the executable's directory to the child's PATH so the OS will resolve
            // the shortName to the correct absolute file we already found.
            if (parentDir != null) {
                String origPath = System.getenv("PATH");
                String newPath = parentDir + File.pathSeparator + (origPath != null ? origPath : "");
                pb.environment().put("PATH", newPath);
            }

            // Inherit IO so the external program's stdout/stderr/stdin show in our console
            //Without this line we have to add extra code to our program to print the output in the
            //same console ,With it, it's automatic and easy.
            pb.inheritIO();

            // it is like a remote control for the pb.start function
            //process is a class
            //Why do you need that remote control? For the very next line
            // p.waitFor(), which is like using the remote to ask, "Are you done yet?"
            Process p = pb.start();

            // Wait for the program to finish before returning to the shell prompt
            p.waitFor();
        } catch (Exception e) {
            System.out.println("Error running program: " + e.getMessage());
        }
    }

}
