import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");
        Scanner sc = new Scanner(System.in);
        String commands = sc.nextLine();
        System.out.println(commands+": command not found");
    }
}
