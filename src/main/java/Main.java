import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            String[] commands = command.split(" ");
            if (command.equals("exit 0")) break;
            else if(command.startsWith("type") && ((command.endsWith("echo")) || (command.endsWith("exit 0")) ||  (command.endsWith("exit")) || (command.endsWith("type")))) {
                System.out.println(commands[1]+" is a shell builtin");
            }
            else if(command.startsWith("type") && ((!command.endsWith("echo")) || (!command.endsWith("exit 0")) ||  (!command.endsWith("exit")) || (!command.endsWith("type")))){
                System.out.println(commands[1]+" not found");
            }
            else if(command.contains("echo")){
                for(int i=1;i<commands.length;i++){
                    System.out.print(commands[i]+" ");
                }
                System.out.println();
            }
            else {
                System.out.println(command + ": command not found");
            }
        }
    }
}