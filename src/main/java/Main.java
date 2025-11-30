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

            String[] raw = splitthestring(command);
            if (raw.length == 0) continue;

            if (raw[0].equals("exit") && (raw.length == 1 || (raw.length > 1 && raw[1].equals("0"))))
                break;

            List<String> argsList = new ArrayList<>();
            String outRedirectPath = null;
            String errRedirectPath = null;

            // ---------------- PARSE REDIRECTION ----------------
            for (int i = 0; i < raw.length; i++) {
                String tok = raw[i];

                if (tok.equals(">") || tok.equals("1>")) {
                    if (i + 1 < raw.length) outRedirectPath = raw[++i];
                    continue;
                }
                if (tok.startsWith("1>") || tok.startsWith(">")) {
                    outRedirectPath = tok.substring(tok.indexOf('>') + 1);
                    continue;
                }

                if (tok.equals("2>")) {
                    if (i + 1 < raw.length) errRedirectPath = raw[++i];
                    continue;
                }
                if (tok.startsWith("2>")) {
                    errRedirectPath = tok.substring(2);
                    continue;
                }

                argsList.add(tok);
            }

            if (argsList.isEmpty()) continue;
            String cmd = argsList.get(0);

            // ---------------- BUILTINS ----------------

            if (cmd.equals("type")) {
                if (argsList.size() < 2) {
                    printOrErr("type: missing argument", outRedirectPath, errRedirectPath);
                    continue;
                }
                String result = type_and_path_handling(argsList.get(1));
                printOrErr(result, outRedirectPath, errRedirectPath);
                continue;
            }

            if (cmd.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < argsList.size(); i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(argsList.get(i));
                }
                printOrErr(sb.toString(), outRedirectPath, errRedirectPath);
                continue;
            }

            if (cmd.equals("pwd")) {
                printOrErr(curr_dir, outRedirectPath, errRedirectPath);
                continue;
            }

            if (cmd.equals("cd")) {
                if (argsList.size() < 2) {
                    System.out.println("cd: missing argument");
                    continue;
                }
                String target = argsList.get(1);
                if (target.equals("~")) target = System.getenv("HOME");

                File dir = new File(target);
                if (!dir.isAbsolute()) dir = new File(curr_dir, target);

                try {
                    File canonical = dir.getCanonicalFile();
                    if (canonical.exists() && canonical.isDirectory()) {
                        curr_dir = canonical.getAbsolutePath();
                    } else {
                        printOrErr("cd: " + target + ": No such file or directory",
                                outRedirectPath, errRedirectPath);
                    }
                } catch (Exception e) {
                    printOrErr("cd: " + target + ": No such file or directory",
                            outRedirectPath, errRedirectPath);
                }
                continue;
            }

            // ---------------- EXTERNAL COMMANDS ----------------

            String path = findExecutable(cmd);
            if (path == null) {
                printOrErr(cmd + ": command not found", outRedirectPath, errRedirectPath);
                continue;
            }

            try {
                List<String> execArgs = new ArrayList<>(argsList);
                ProcessBuilder pb = new ProcessBuilder(execArgs);

                pb.directory(new File(curr_dir));

                // PATH ENV FIX
                File execFile = new File(path);
                String parent = execFile.getParent();
                if (parent != null) {
                    String orig = System.getenv("PATH");
                    pb.environment().put("PATH", parent + File.pathSeparator + orig);
                }

                // STDOUT redirection
                if (outRedirectPath != null) pb.redirectOutput(new File(outRedirectPath));
                else pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

                // STDERR redirection
                if (errRedirectPath != null) pb.redirectError(new File(errRedirectPath));
                else pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

                Process p = pb.start();
                p.waitFor();

            } catch (Exception e) {
                printOrErr("Error running program: " + e.getMessage(), outRedirectPath, errRedirectPath);
            }
        }
    }

    // ---------------- PRINT HANDLING FOR BUILTINS ----------------
    static void printOrErr(String msg, String out, String err) {
        if (err != null) writeToFile(err, msg + "\n");
        else if (out != null) writeToFile(out, msg + "\n");
        else System.out.println(msg);
    }

    static void writeToFile(String path, String content) {
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (PrintStream ps = new PrintStream(new FileOutputStream(f, false))) {
                ps.print(content);
            }
        } catch (Exception e) {
            System.out.println("write error: " + e.getMessage());
        }
    }

    // ---------------- TOKENIZER (quotes + backslashes FULL SUPPORT) ----------------
    public static String[] splitthestring(String c) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean s = false, d = false;

        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);

            if (ch == '\'' && !d) { s = !s; continue; }
            if (ch == '"' && !s) { d = !d; continue; }

            if (ch == '\\') {
                if (d && i + 1 < c.length()) {
                    char nx = c.charAt(i + 1);
                    if (nx == '\\' || nx == '"') {
                        cur.append(nx); i++; continue;
                    }
                    cur.append('\\'); continue;
                }
                if (!s && !d && i + 1 < c.length()) {
                    cur.append(c.charAt(i + 1)); i++; continue;
                }
                cur.append('\\'); continue;
            }

            if (Character.isWhitespace(ch) && !s && !d) {
                if (cur.length() > 0) { list.add(cur.toString()); cur.setLength(0); }
                continue;
            }

            cur.append(ch);
        }
        if (cur.length() > 0) list.add(cur.toString());
        return list.toArray(new String[0]);
    }

    // ---------------- PATH + EXEC ----------------
    public static String type_and_path_handling(String command) {
        String[] built = {"exit","echo","type","pwd","cd"};
        for (String b : built) if (b.equals(command)) return command+" is a shell builtin";

        String path = findExecutable(command);
        return path != null ? command+" is "+path : command+": not found";
    }

    public static String findExecutable(String cmd) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        for (String d : pathEnv.split(File.pathSeparator)) {
            if (d.isEmpty()) d=".";
            File f = new File(d, cmd);
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }
}
