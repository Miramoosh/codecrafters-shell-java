import java.io.File;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("$ ");
            String command = scanner.nextLine();
            String[] commands = command.split(" ");

            //exit condition
            if (command.equals("exit 0") || command.equals("exit")) break;

            //type command
            else if(command.contains("type")){
                String a=type_and_path_handling(commands[1]);
                System.out.println(a);
            }
            //echo command
            else if(command.contains("echo")){
                for(int i=1;i<commands.length;i++){
                    System.out.print(commands[i]+" ");
                }
                System.out.println();
            }

            //invalid command
            else {
                System.out.println(command + ": command not found");
            }
        }
    }

    public static String type_and_path_handling(String command){
        String[] cmd={"exit","echo","type"};
        String pathEnv = System.getenv("PATH");
        String[] commands = pathEnv.split(";");
        for(String i:cmd){
            if(Objects.equals(i,command)){
                return i +" is a shell builtin";
            }
        }
        for(int j=0;j<commands.length;j++){
            File file=new File(commands[j],command);
            if(file.exists()){
                return command + " is " + file.getAbsolutePath();
            }
        }
        return command + ": not found";
    }
}