import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String curr_dir = System.getProperty("user.dir");

        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] raw = input.split(" ");
            List<String> cmd = new ArrayList<>();
            String outFile = null, errFile = null;

            // ---------------- Parse for redirection ----------------
            for (int i = 0; i < raw.length; i++) {
                if (raw[i].equals(">") || raw[i].equals("1>"))
                    outFile = raw[++i];
                else if (raw[i].equals("2>"))
                    errFile = raw[++i];
                else
                    cmd.add(raw[i]);
            }

            if (cmd.size() == 0) continue;
            String command = cmd.get(0);


            // ---------------- EXIT ----------------
            if (command.equals("exit") || input.equals("exit 0"))
                break;


                // ---------------- FIXED - ECHO (stderr redirection) ----------------
            else if (command.equals("echo")) {

                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < cmd.size(); i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(cmd.get(i));
                }
                String result = sb.toString() + "\n";

                if (errFile != null) writeToFile(errFile, result);  // 2> output → file
                else if (outFile != null) writeToFile(outFile, result); // > or 1> → file
                else System.out.print(result); // no redirection → print normally

                continue;
            }


            // ---------------- CD ----------------
            else if (command.equals("cd")) {
                if (cmd.size() < 2) { System.out.println("cd: missing argument"); continue; }

                String target = cmd.get(1);
                if (target.equals("~")) target = System.getenv("HOME");

                File dir = new File(target);
                if (!dir.isAbsolute()) dir = new File(curr_dir, target);

                if (dir.exists() && dir.isDirectory())
                    curr_dir = dir.getCanonicalPath();
                else
                    System.out.println("cd: " + target + ": No such file or directory");

                continue;
            }


            // ---------------- PWD ----------------
            else if (command.equals("pwd")) {
                System.out.println(curr_dir);
                continue;
            }


            // ---------------- TYPE ----------------
            else if (command.equals("type")) {
                System.out.println(typeOf(cmd.get(1)));
                continue;
            }


            // ---------------- EXTERNAL COMMAND ----------------
            else {
                String exec = findExecutable(cmd.get(0));
                if (exec != null)
                    run(exec, cmd, curr_dir, outFile, errFile);
                else
                    System.out.println(command + ": command not found");
            }
        }
    }


    // ============= TYPE BUILTIN =============
    static String typeOf(String c) {
        String[] builtins = {"echo","cd","pwd","exit","type"};
        for (String x : builtins)
            if (x.equals(c))
                return c + " is a shell builtin";

        String p = findExecutable(c);
        return p != null ? c+" is "+p : c+": not found";
    }


    // ============= PATH LOOKUP =============
    static String findExecutable(String name) {
        String[] path = System.getenv("PATH").split(File.pathSeparator);
        for (String d : path) {
            File f = new File(d, name);
            if (f.exists() && f.isFile() && f.canExecute())
                return f.getAbsolutePath();
        }
        return null;
    }


    // ============= RUN EXTERNAL COMMANDS =============
    static void run(String exec, List<String> args, String dir,
                    String outFile, String errFile) {

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(dir));

            if (outFile != null) pb.redirectOutput(new File(outFile));
            if (errFile != null) pb.redirectError(new File(errFile));

            if (outFile == null && errFile == null)
                pb.inheritIO(); // only inherit when nothing redirected

            Process p = pb.start();
            p.waitFor();
        } catch (Exception ignored) {}
    }


    // ============= WRITE TO FILE =============
    static void writeToFile(String file, String text) {
        try (FileWriter f = new FileWriter(file)) { f.write(text); }
        catch (Exception ignored) {}
    }
}
