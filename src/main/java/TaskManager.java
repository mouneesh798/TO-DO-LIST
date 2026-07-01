import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final String FILE_NAME = "tasks.txt";

    public TaskManager() {
        loadTasks();
    }

    public void addTask(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            System.out.println("Task name cannot be empty.");
            return;
        }

        tasks.add(new Task(taskName.trim(), false));
        saveTasks();
        System.out.println("Task Added Successfully!");
    }

    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    public void viewTasks() {
        if (tasks.isEmpty()) {
            System.out.println("No Tasks Found.");
            return;
        }

        System.out.println("\n------ TASK LIST ------");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i + 1) + ". " + tasks.get(i));
        }
    }

    public void deleteTask(int index) {
        if (index < 1 || index > tasks.size()) {
            System.out.println("Invalid Task Number!");
            return;
        }

        tasks.remove(index - 1);
        saveTasks();
        System.out.println("Task Deleted Successfully!");
    }

    public void markCompleted(int index) {
        if (index < 1 || index > tasks.size()) {
            System.out.println("Invalid Task Number!");
            return;
        }

        tasks.get(index - 1).markCompleted();
        saveTasks();
        System.out.println("Task Marked Completed!");
    }

    private void saveTasks() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Task task : tasks) {
                bw.write(task.getTaskName() + "," + task.isCompleted());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error Saving Tasks.");
        }
    }

    private void loadTasks() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= 2) {
                    tasks.add(new Task(data[0], Boolean.parseBoolean(data[1])));
                }
            }
        } catch (IOException e) {
            System.out.println("Error Loading Tasks.");
        }
    }
}
