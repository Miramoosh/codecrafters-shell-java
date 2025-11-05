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

            // exit condition
            if (command.equals("exit 0") || command.equals("exit")) break;

                // type command
            else if (command.contains("type")) {
                String a = type_and_path_handling(commands[1]);
                System.out.println(a);
            }

            // echo command
            else if (command.contains("echo")) {
                for (int i = 1; i < commands.length; i++) {
                    System.out.print(commands[i] + " ");
                }
                System.out.println();
            }

            // NEW PART 👇 (run external programs)
            else {
                String executablePath = findExecutable(commands[0]); // search PATH
                if (executablePath != null) {
                    runExternalProgram(executablePath, commands); // run it with args
                } else {
                    System.out.println(commands[0] + ": command not found");
                }
            }
        }
    }

    // same function as before
    public static String type_and_path_handling(String command) {
        String[] cmd = {"exit", "echo", "type"};
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

    // ✅ NEW FUNCTION: finds full executable path
    public static String findExecutable(String command) {
        String pathEnv = System.getenv("PATH");
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

    // ✅ NEW FUNCTION: executes external programs
    public static void runExternalProgram(String executablePath, String[] args) {
        try {
            List<String> commandList = new ArrayList<>();
            commandList.add(executablePath);
            for (int i = 1; i < args.length; i++) {
                commandList.add(args[i]);
            }

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.inheritIO();  // shows the output of external program
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Error running program: " + e.getMessage());
        }
    }
}
