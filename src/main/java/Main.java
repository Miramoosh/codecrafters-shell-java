import java.io.File;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            String[] commands = command.split(" ");

            //exit condition
            if (command.equals("exit 0") || command.equals("exit")) break;
            
            //pwd command
            else if (command.equals("pwd")) {
                String a=System.getProperty("user.dir");
                System.out.println(a);
            }
            //type command
            else if (command.contains("type")) {
                String a = type_and_path_handling(commands[1]);
                System.out.println(a);
            }
            //echo command
            else if (command.contains("echo")) {
                for (int i = 1; i < commands.length; i++) {
                    System.out.print(commands[i] + " ");
                }
                System.out.println();
            }

            // external programs (search PATH and run)
            else {
                String executablePath = findExecutable(commands[0]);
                if (executablePath != null) {
                    runExternalProgram(executablePath, commands);
                } else {
                    System.out.println(commands[0] + ": command not found");
                }
            }
        }
    }

    public static String type_and_path_handling(String command) {
        String[] cmd = {"exit", "echo", "type","pwd"};
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

    /**
     * Run external program found at executablePath.
     *
     * Important: to make argv[0] inside the child be the short program name (what the user typed),
     * we pass the short name as the first command argument and modify the child's PATH so the
     * found directory is searched first. This ensures the child process runs the exact file we found
     * while receiving argv[0] equal to the short name (e.g. "custom_exe_1234").
     */
    public static void runExternalProgram(String executablePath, String[] args) {
        try {
            File execFile = new File(executablePath);
            String parentDir = execFile.getParent();
            String shortName = args[0]; // what the user typed (program name)

            // Build command array where argv[0] is the short name (user typed)
            List<String> cmdList = new ArrayList<>();
            cmdList.add(shortName);
            for (int i = 1; i < args.length; i++) {
                cmdList.add(args[i]);
            }

            ProcessBuilder pb = new ProcessBuilder(cmdList);

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

            // Wait for the program to finish before returning to the shell prompt
            p.waitFor();
        } catch (Exception e) {
            System.out.println("Error running program: " + e.getMessage());
        }
    }
}
