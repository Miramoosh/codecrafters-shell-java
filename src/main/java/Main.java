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

            // ---------------- Parse arguments ----------------
            for (int i = 0; i < raw.length; i++) {
                if (raw[i].equals(">") || raw[i].equals("1>")) outFile = raw[++i];
                else if (raw[i].equals("2>")) errFile = raw[++i];
                else cmd.add(raw[i]);
            }

            if (cmd.size() == 0) continue;
            String command = cmd.get(0);

            // ---------------- EXIT ----------------
            if (command.equals("exit") || input.equals("exit 0")) break;

                // ---------------- ECHO (FIXED) ----------------
            else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < cmd.size(); i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(cmd.get(i));
                }

                String result = sb.toString() + "\n";

                if (errFile != null)       write(errFile, result);   // 2> redirection
                else if (outFile != null)  write(outFile, result);   // >, 1>
                else                       System.out.print(result); // normal

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
                System.out.println(type(cmd.get(1)));
                continue;
            }

            // ---------------- External Commands ----------------
            else {
                String exec = find(cmd.get(0));
                if (exec != null) run(exec, cmd, curr_dir, outFile, errFile);
                else System.out.println(command + ": command not found");
            }
        }
    }

    // ---------------- TYPE HANDLER ----------------
    static String type(String c) {
        String[] builtins = {"echo","cd","pwd","exit","type"};
        for (String b : builtins)
            if (b.equals(c)) return c + " is a shell builtin";

        String p = find(c);
        return (p != null) ? c + " is " + p : c + ": not found";
    }

    // ---------------- Find executable in PATH ----------------
    static String find(String c) {
        for (String dir : System.getenv("PATH").split(File.pathSeparator)) {
            File f = new File(dir, c);
            if (f.exists() && f.isFile() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    // ---------------- Run external program with redirection ----------------
    static void run(String exec, List<String> args, String cwd,
                    String outFile, String errFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(cwd));

            if (outFile != null) pb.redirectOutput(new File(outFile));
            if (errFile != null) pb.redirectError(new File(errFile));

            if (outFile == null && errFile == null) pb.inheritIO();

            Process p = pb.start();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    // ---------------- File writer for redirection ----------------
    static void write(String file, String text) {
        try (FileWriter fw = new FileWriter(file)) { fw.write(text); }
        catch (Exception ignored) {}
    }
}
