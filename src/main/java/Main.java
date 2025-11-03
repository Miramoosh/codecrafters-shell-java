import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String commands = sc.nextLine();
        while(true){
            String command = sc.nextLine();
            System.out.print("$ ");
            System.out.println(commands+": command not found");
        }
    }
}
