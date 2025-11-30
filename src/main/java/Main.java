import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        String curr_dir = System.getProperty("user.dir");

        while (true) {

            System.out.print("$ ");
            String raw = scanner.nextLine();

            if (raw.equals("exit") || raw.equals("exit 0")) break;

            String[] parts = parse(raw);
            if (parts.length == 0) continue;

            // Detect redirection
            String outFile = null, errFile = null;
            List<String> cmd = new ArrayList<>();

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(">") || parts[i].equals("1>")) {
                    outFile = parts[i + 1];
                    i++;
                } else if (parts[i].equals("2>")) {
                    errFile = parts[i + 1];
                    i++;
                } else cmd.add(parts[i]);
            }

            // Final command list to execute
            String[] commands = cmd.toArray(new String[0]);
            if (commands.length == 0) continue;

            /* ================= BUILTINS ================= */

            if (commands[0].equals("type")) {
                String res = typeAndPath(commands[1]);
                printOrRedirect(res, outFile, errFile);
                continue;
            }

            if (commands[0].equals("echo")) {
                String msg = String.join(" ", Arrays.copyOfRange(commands, 1, commands.length));
                printOrRedirect(msg, outFile, errFile);
                continue;
            }

            if (commands[0].equals("pwd")) {
                printOrRedirect(curr_dir, outFile, errFile);
                continue;
            }

            if (commands[0].equals("cd")) {
                if (commands.length < 2) {
                    printErr("cd: missing argument", errFile);
                    continue;
                }
                String path = commands[1];

                if (path.equals("~")) path = System.getenv("HOME");

                File dir = new File(path);
                if (!dir.isAbsolute()) dir = new File(curr_dir, path);

                if (dir.exists() && dir.isDirectory()) curr_dir = dir.getCanonicalPath();
                else printErr("cd: " + path + ": No such file or directory", errFile);
                continue;
            }

            /* ================= EXECUTABLE PROGRAM ================= */

            String exec = findExecutable(commands[0]);

            if (exec == null) {
                printErr(commands[0] + ": command not found", errFile);
                continue;
            }

            run(exec, commands, curr_dir, outFile, errFile);
        }
    }

    /* ===================== STRING SPLITTER ===================== */

    public static String[] parse(String s) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean sQ = false, dQ = false, esc = false;

        for (char c : s.toCharArray()) {
            if (esc) {
                cur.append(c);
                esc = false;
            }
            else if (c == '\\' && dQ) esc = true;
            else if (c == '\'' && !dQ) sQ = !sQ;
            else if (c == '"' && !sQ) dQ = !dQ;
            else if (Character.isWhitespace(c) && !sQ && !dQ) {
                if (cur.length() > 0) { list.add(cur.toString()); cur.setLength(0); }
            }
            else cur.append(c);
        }
        if (cur.length() > 0) list.add(cur.toString());
        return list.toArray(new String[0]);
    }

    /* ================= PATH + TYPE ================= */

    static String typeAndPath(String cmd) {
        String[] builtins = {"echo", "cd", "pwd", "type", "exit"};
        for (String b : builtins) if (b.equals(cmd)) return b + " is a shell builtin";

        String exec = findExecutable(cmd);
        return (exec != null) ? cmd + " is " + exec : cmd + ": not found";
    }

    static String findExecutable(String cmd) {
        for (String dir : System.getenv("PATH").split(File.pathSeparator)) {
            File f = new File(dir, cmd);
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    /* ================= RUN EXTERNAL PROGRAM ================= */

    static void run(String exec, String[] args, String dir, String outFile, String errFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(dir));

            if (outFile != null) pb.redirectOutput(new File(outFile));
            if (errFile != null) pb.redirectError(new File(errFile));

            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();

        } catch (Exception e) {}
    }

    /* ================= REDIRECTION UTIL ================= */

    static void printOrRedirect(String msg, String out, String err) {
        if (err != null) write(err, msg);
        else if (out != null) write(out, msg);
        else System.out.println(msg);
    }

    static void printErr(String msg, String errFile) {
        if (errFile != null) write(errFile, msg);
        else System.err.println(msg);
    }

    static void write(String file, String data) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(data + "\n");
        } catch (Exception ignored) {}
    }
}
