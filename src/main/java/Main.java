import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        boolean b = true;
        while (b) {
            System.out.print("$ ");
            String input = scanner.nextLine();

            String[] words = input.split(" ");
            String command = words[0];
            String[] rest = Arrays.copyOfRange(words, 1, words.length);

            String result = String.join(" ", rest);

            if (Objects.equals(command, "exit")) {
                b = false;
            } else if (Objects.equals(command, "echo")) {
                System.out.println(result);
            } else if (command.equals("type")) {
                System.out.println(type(result));
            } else {
                System.out.println(input + ": command not found");
            }
        }

        scanner.close();
    }

    public static String type(String command) {
        String[] commands = {"exit", "echo", "type"};
        String path_commands = System.getenv("PATH");
        String[] path_command = path_commands.split(":");

        boolean isTrue = false;
        for (int i = 0; i < commands.length; i++) {
            if (Objects.equals(commands[i], command)) {
                return command + " is a shell builtin";
            }
        }
        for (int i = 0; i < path_command.length; i++) {
            File file = new File(path_command[i], command);
            if (file.exists()) {
                return command + " is " + file.getAbsolutePath();
            }
        }

        return command + ": not found";
    }
}