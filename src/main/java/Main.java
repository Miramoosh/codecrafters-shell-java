import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String curr_dir = System.getProperty("user.dir");

        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            if (command == null) command = "";
            command = command.trim();

            // Tokenize (quote/backslash aware)
            String[] raw = splitthestring(command);
            if (raw.length == 0) continue;

            // exit handling: "exit" or "exit 0"
            if (raw[0].equals("exit") && (raw.length == 1 || (raw.length > 1 && raw[1].equals("0")))) {
                break;
            }

            // Parse redirection and build argument list (argsList)
            List<String> argsList = new ArrayList<>();
            String outRedirectPath = null;

            for (int i = 0; i < raw.length; i++) {
                String tok = raw[i];

                // exact tokens: ">" or "1>"
                if (tok.equals(">") || tok.equals("1>")) {
                    if (i + 1 < raw.length) {
                        outRedirectPath = raw[++i];
                    }
                    // else ignore (malformed) - no filename
                    continue;
                }

                // combined forms like ">filename" or "1>filename"
                if (tok.startsWith("1>") || tok.startsWith(">")) {
                    int idx = tok.indexOf('>');
                    String pathPart = tok.substring(idx + 1);
                    if (!pathPart.isEmpty()) {
                        outRedirectPath = pathPart;
                        continue;
                    } else {
                        // if nothing after '>', look for next token (already handled above)
                        continue;
                    }
                }

                // Normal argument
                argsList.add(tok);
            }

            if (argsList.size() == 0) {
                // nothing to execute (e.g., only redirection) — skip
                continue;
            }

            // Now dispatch commands based on argsList
            String cmd0 = argsList.get(0);

            // type builtin
            if (cmd0.equals("type")) {
                if (argsList.size() < 2) {
                    System.out.println("type: missing argument");
                    continue;
                }
                String res = type_and_path_handling(argsList.get(1));
                if (outRedirectPath != null) {
                    writeStringToFile(outRedirectPath, res + System.lineSeparator());
                } else {
                    System.out.println(res);
                }
            }

            // echo builtin
            else if (cmd0.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < argsList.size(); i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(argsList.get(i));
                }
                String out = sb.toString();
                if (outRedirectPath != null) {
                    writeStringToFile(outRedirectPath, out + System.lineSeparator());
                } else {
                    System.out.println(out);
                }
            }

            // cd builtin
            else if (cmd0.equals("cd")) {
                if (argsList.size() < 2) {
                    System.out.println("cd: missing argument");
                    continue;
                }
                String target_dir = argsList.get(1);
                if (target_dir.equals("~")) {
                    target_dir = System.getenv("HOME");
                }
                File dir = new File(target_dir);
                if (!dir.isAbsolute()) {
                    dir = new File(curr_dir, target_dir);
                }
                try {
                    File canonical = dir.getCanonicalFile();
                    if (canonical.exists() && canonical.isDirectory()) {
                        curr_dir = canonical.getAbsolutePath();
                    } else {
                        System.out.println("cd: " + argsList.get(1) + ": No such file or directory");
                    }
                } catch (java.io.IOException e) {
                    System.out.println("cd: " + argsList.get(1) + ": No such file or directory");
                }
            }

            // pwd builtin
            else if (cmd0.equals("pwd")) {
                if (outRedirectPath != null) {
                    writeStringToFile(outRedirectPath, curr_dir + System.lineSeparator());
                } else {
                    System.out.println(curr_dir);
                }
            }

            // external programs
            else {
                String executablePath = findExecutable(cmd0);
                if (executablePath != null) {
                    // Build cmdList from argsList (these are already parsed tokens)
                    List<String> cmdList = new ArrayList<>(argsList);

                    if (outRedirectPath != null) {
                        // Run with stdout redirected to file (stderr still inherited)
                        try {
                            File execFile = new File(executablePath);
                            String parentDir = execFile.getParent();

                            ProcessBuilder pb = new ProcessBuilder(cmdList);
                            pb.directory(new File(curr_dir));

                            if (parentDir != null) {
                                String origPath = System.getenv("PATH");
                                String newPath = parentDir + File.pathSeparator + (origPath != null ? origPath : "");
                                pb.environment().put("PATH", newPath);
                            }

                            // Ensure parent dirs for output file exist
                            File outFile = new File(outRedirectPath);
                            File parent = outFile.getParentFile();
                            if (parent != null && !parent.exists()) {
                                parent.mkdirs();
                            }

                            pb.redirectOutput(outFile); // stdout -> file (truncated)
                            pb.redirectError(ProcessBuilder.Redirect.INHERIT); // stderr -> console
                            pb.redirectInput(ProcessBuilder.Redirect.INHERIT); // stdin from console
                            Process p = pb.start();
                            p.waitFor();
                        } catch (Exception e) {
                            System.out.println("Error running program: " + e.getMessage());
                        }
                    } else {
                        // No redirection — preserve existing behavior
                        // Convert cmdList to array and call runExternalProgram
                        String[] arr = cmdList.toArray(new String[0]);
                        runExternalProgram(executablePath, arr, curr_dir);
                    }
                } else {
                    System.out.println(cmd0 + ": command not found");
                }
            }
        } // end while
    } // end main

    // Helper: write a string to a file (create parent dirs), overwrite file
    public static void writeStringToFile(String path, String content) {
        try {
            File outFile = new File(path);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outFile, false);
                 PrintStream ps = new PrintStream(fos)) {
                ps.print(content);
            }
        } catch (Exception e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    // Tokenizer that supports quotes and backslash rules (keeps behavior from previous stages)
    public static String[] splitthestring(String command) {
        List<String> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);

            // Toggle single-quote mode only when not inside double quotes
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue; // don't include the quote character
            }

            // Toggle double-quote mode only when not inside single quotes
            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                continue; // don't include the quote character
            }

            // Backslash handling:
            // - Outside any quotes: backslash escapes any next char
            // - Inside single quotes: backslash is literal
            // - Inside double quotes: backslash escapes only " and \ (per current stage)
            if (ch == '\\') {
                // inside double quotes: only \" and \\ are special (escape)
                if (inDouble) {
                    if (i + 1 < command.length()) {
                        char next = command.charAt(i + 1);
                        if (next == '"' || next == '\\') {
                            current.append(next);
                            i++;
                            continue;
                        } else {
                            // backslash remains literal inside double quotes for other chars
                            current.append('\\');
                            continue;
                        }
                    } else {
                        current.append('\\');
                        continue;
                    }
                }

                // outside quotes: escape next char (if any)
                if (!inSingle && !inDouble) {
                    if (i + 1 < command.length()) {
                        current.append(command.charAt(i + 1));
                        i++;
                        continue;
                    } else {
                        current.append('\\');
                        continue;
                    }
                }

                // inside single quotes: literal backslash
                current.append('\\');
                continue;
            }

            // Whitespace outside any quotes splits tokens
            if (Character.isWhitespace(ch) && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    list.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            // Normal character: append
            current.append(ch);
        }

        if (current.length() > 0) {
            list.add(current.toString());
        }

        return list.toArray(new String[0]);
    }

    public static String type_and_path_handling(String command) {
        String[] cmd = {"exit", "echo", "type", "pwd", "cd"};
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

    public static void runExternalProgram(String executablePath, String[] args, String currentDirectory) {
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

            // set child's working directory
            pb.directory(new File(currentDirectory));
            // Prepend the executable's directory to the child's PATH so the OS will resolve
            // the shortName to the correct absolute file we already found.
            if (parentDir != null) {
                String origPath = System.getenv("PATH");
                String newPath = parentDir + File.pathSeparator + (origPath != null ? origPath : "");
                pb.environment().put("PATH", newPath);
            }

            // Inherit IO so the external program's stdout/stderr/stdin show in our console
            pb.inheritIO();

            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            System.out.println("Error running program: " + e.getMessage());
        }
    }

}
