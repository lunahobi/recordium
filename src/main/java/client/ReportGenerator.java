package client;

import java.io.IOException;

public class ReportGenerator {

    public void generateReport() {
        try {
            // Генерация Allure-отчета
            ProcessBuilder pb = new ProcessBuilder("C:\\Users\\79673\\Downloads\\allure-2.32.0\\allure-2.32.0\\bin\\allure.bat", "generate", "target/reports/allure-results", "-o", "target/reports/allure-reports");
            pb.inheritIO(); // Для вывода логов в консоль
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Allure report generated successfully.");
            } else {
                System.err.println("Failed to generate Allure report. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to generate Allure report.");
            e.printStackTrace();
        }
    }

    public void openReportInBrowser() throws IOException {
        String mavenPath = "C:\\Users\\79673\\Downloads\\apache-maven-3.9.9-bin\\apache-maven-3.9.9\\bin\\mvn.cmd";
        ProcessBuilder pb = new ProcessBuilder(mavenPath, "allure:serve");
        pb.inheritIO();
        pb.start();
    }

}
