import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        TaskManager manager = new TaskManager();

        while (true) {
            System.out.println("\n========= TO DO LIST =========");
            System.out.println("1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Delete Task");
            System.out.println("4. Mark Completed");
            System.out.println("5. Exit");
            System.out.print("Enter Choice : ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter Task : ");
                    String task = sc.nextLine();
                    manager.addTask(task);
                    break;
                case 2:
                    manager.viewTasks();
                    break;
                case 3:
                    manager.viewTasks();
                    System.out.print("Enter Task Number : ");
                    int delete = sc.nextInt();
                    manager.deleteTask(delete);
                    break;
                case 4:
                    manager.viewTasks();
                    System.out.print("Enter Task Number : ");
                    int complete = sc.nextInt();
                    manager.markCompleted(complete);
                    break;
                case 5:
                    System.out.println("Thank You!");
                    sc.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid Choice!");
            }
        }
    }
}
