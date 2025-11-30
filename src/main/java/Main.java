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

            String[] parts = input.split(" ");
            List<String> cmd = new ArrayList<>();
            String outFile = null, errFile = null;

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(">") || parts[i].equals("1>")) {
                    outFile = parts[++i];
                }
                else if (parts[i].equals("2>")) {
                    errFile = parts[++i];
                }
                else cmd.add(parts[i]);
            }

            if (cmd.size() == 0) continue;
            String command = cmd.get(0);

            if (command.equals("exit") || input.equals("exit 0")) break;

                // ---------------- ECHO ----------------
            else if (command.equals("echo")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < cmd.size(); i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(cmd.get(i));
                }

                if (errFile != null) write(errFile, sb + "\n");
                else if (outFile != null) write(outFile, sb + "\n");
                else System.out.println(sb);
            }

            // ---------------- CD ----------------
            else if (command.equals("cd")) {
                if (cmd.size() < 2) { System.out.println("cd: missing argument"); continue; }

                String target = cmd.get(1);

                if (target.equals("~")) target = System.getenv("HOME");
                File dir = new File(target);
                if (!dir.isAbsolute()) dir = new File(curr_dir, target);

                if (dir.exists() && dir.isDirectory()) curr_dir = dir.getCanonicalPath();
                else System.out.println("cd: " + target + ": No such file or directory");
            }

            // ---------------- PWD ----------------
            else if (command.equals("pwd")) {
                System.out.println(curr_dir);
            }

            // ---------------- TYPE ----------------
            else if (command.equals("type")) {
                System.out.println(type(cmd.get(1)));
            }

            // ---------------- EXTERNAL COMMANDS ----------------
            else {
                String exec = find(cmd.get(0));
                if (exec != null) run(exec, cmd, curr_dir, outFile, errFile);
                else System.out.println(command + ": command not found");
            }
        }
    }

    static String type(String c) {
        String[] b = {"echo","cd","pwd","exit","type"};
        for (String x : b) if (x.equals(c)) return c + " is a shell builtin";
        String e = find(c);
        return (e != null) ? c+" is "+e : c+": not found";
    }

    static String find(String cmd) {
        String path = System.getenv("PATH");
        for (String dir : path.split(File.pathSeparator))
        {
            File f = new File(dir, cmd);
            if (f.exists() && f.isFile() && f.canExecute()) return f.getAbsolutePath();
        }
        return null;
    }

    static void run(String exec, List<String> args, String dir,
                    String outFile, String errFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(dir));

            if (outFile != null) pb.redirectOutput(new File(outFile));
            if (errFile != null) pb.redirectError(new File(errFile));

            if (outFile == null && errFile == null) pb.inheritIO();

            Process p = pb.start();
            p.waitFor();

        } catch(Exception e){}
    }

    static void write(String file, String text) {
        try (FileWriter fw = new FileWriter(file)) { fw.write(text); }
        catch(Exception ignored){}
    }
}
