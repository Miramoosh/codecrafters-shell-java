import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            if (command.equals("exit 0")) break;
            String[] commands = command.split(" ");
            if(command.contains("echo")){
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