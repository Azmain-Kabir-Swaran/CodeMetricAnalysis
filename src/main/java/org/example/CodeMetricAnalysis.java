package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import raykernel.apps.readability.eval.Main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class CodeMetricAnalysis {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Please make sure you give your command properly, as shown below:\njava -jar 'target/CodeMetricAnalysis-1.0-SNAPSHOT-jar-with-dependencies.jar' <input_directory> <output_directory>");
            return;
        }

        String inputDirectory = args[0];
        String outputDirectory = args[1];

        String[] projects = {"checkstyle", "hadoop"};

        // Joining 'checkstyle' and 'hadoop' project paths
        for (String project : projects) {
            Path projectPath = Paths.get(inputDirectory, project);
            try (Stream<Path> paths = Files.walk(projectPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> processJsonFile(path.toString(), outputDirectory));
            } catch (IOException e) {
                System.err.println("Error processing project: " + project);
                e.printStackTrace();
            }
        }
    }

    private static void processJsonFile(String jsonFilePath, String outputDirectory) {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader(jsonFilePath);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            // Extract necessary data from JSON
            List<String> changeHistory = (List<String>) data.get("changeHistory");

            Map<String, Map<String, Object>> changeHistoryDetails = (Map<String, Map<String, Object>>) data.get("changeHistoryDetails");

            // Filter out 'non-YIntroduced' commits and get the 'Yintroduced' commit
            Map<String, Object> introductionCommit = null;
            for (String commitId : changeHistory) {
                Map<String, Object> commitDetails = changeHistoryDetails.get(commitId);
                if (commitDetails.get("type").toString().contains("Yintroduced")) {
                    introductionCommit = commitDetails;
                    break;
                }
            }

            if (introductionCommit == null) {
                System.out.println("No introduction commit found for method in " + jsonFilePath);
                return;
            }

            String sourceCode = (String) introductionCommit.get("actualSource");

            // Wrap the source code with a dummy class so that it can be parsed using Java parser where necessary
            String wrappedSourceCode = wrapMethodInClass(sourceCode.trim());

            try {
                CompilationUnit compilationUnit = JavaParser.parse(wrappedSourceCode);
                int size = calculateSLOC(compilationUnit);
                int mccabe = calculateMcCabe(compilationUnit);
                double readability = calculateReadability(sourceCode.trim());
                int revisions = countRevisions(changeHistoryDetails);

                // Output to CSV
                String projectName = jsonFilePath.contains("checkstyle") ? "checkstyle" : "hadoop";
                String outputFile = outputDirectory + "/" + projectName + ".csv";

                File file = new File(outputFile);

                // Check if the file already exists and is not empty
                boolean fileExistsAndNotEmpty = file.exists() && file.length() > 0;

                // Use a FileWriter to append data
                FileWriter fileWriter = new FileWriter(file, true);

                // If the file is being created for the first time or is empty, include the header
                CSVFormat csvFormat;
                if (fileExistsAndNotEmpty) {
                    csvFormat = CSVFormat.DEFAULT;
                } else {
                    csvFormat = CSVFormat.DEFAULT.builder()
                            .setHeader("JSON File", "Size", "McCabe", "Readability", "Revisions").build();
                }

                // CSVPrinter to format the CSV output
                try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFormat)) {
                    printer.printRecord(jsonFilePath, size, mccabe, readability, revisions);
                    System.out.println("Successfully saved the data for method in " + jsonFilePath);
                }
            } catch (RuntimeException e) {
                System.out.println("Error parsing source code for " + jsonFilePath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Wrap the source code with a dummy class named "DummyClass" for parsing purpose
    private static String wrapMethodInClass(String methodSource) {
        return "public class DummyClass {\n" + methodSource + "\n}";
    }

    private static int calculateSLOC(CompilationUnit compilationUnit) {
        compilationUnit.removeComment();
        int removedEmptyLine = (int) compilationUnit.toString().lines().filter(line -> !line.trim().isEmpty()).count();

        // Subtracting 2 lines for the dummy class wrapper
        return removedEmptyLine - 2;
    }

    private static int calculateMcCabe(CompilationUnit compilationUnit) {
        McCabeVisitor visitor = new McCabeVisitor();
        visitor.visit(compilationUnit, null);
        return 1 + visitor.getComplexity();
    }

    public static double calculateReadability(String javaMethodCode) {
        Main readabilityTool = new Main();

        // Analyze the readability of the method
        double readabilityScore = readabilityTool.getReadability(javaMethodCode);

        return readabilityScore;
    }

    private static int countRevisions(Map<String, Map<String, Object>> changeHistoryDetails) {
        int revisions = 0;
        String originalSource = null;

        for (Map.Entry<String, Map<String, Object>> entry : changeHistoryDetails.entrySet()) {
            Map<String, Object> details = entry.getValue();
            String changeType = (String) details.get("type");
            String actualSource = (String) details.get("actualSource");

            if (changeType.contains("Yintroduced")) {
                originalSource = actualSource.trim();
                revisions++; // Count the introduction as a revision
            } else if (!changeType.contains("Yfilerename") && !changeType.contains("Ymovefromfile")) {
                revisions++; // Count all other changes as revisions
            } else if ((changeType.contains("Yfilerename") || changeType.contains("Ymovefromfile")) && (originalSource == null || !actualSource.trim().equals(originalSource))) {
                revisions++; // Count renames only if the content was modified
            }
        }

        return revisions;
    }

    public static class McCabeVisitor extends VoidVisitorAdapter<Void> {
        private int complexity = 1; // Start with a base of 1 for the method entry point

        // Visit conditional statements
        @Override
        public void visit(IfStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        @Override
        public void visit(SwitchEntryStmt n, Void arg) {
            // Check if there is a label present for this switch entry (case)
            if (n.getLabel().isPresent()) {
                complexity++; // Increment complexity for each labeled case
            }
            super.visit(n, arg);
        }

        // Visit conditional expressions (ternary operators)
        @Override
        public void visit(ConditionalExpr n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Visit loop statements
        @Override
        public void visit(WhileStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        @Override
        public void visit(ForStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        @Override
        public void visit(ForeachStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        @Override
        public void visit(DoStmt n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Visit catch clauses for exception handling
        @Override
        public void visit(CatchClause n, Void arg) {
            complexity++;
            super.visit(n, arg);
        }

        // Visit binary expressions for logical operators that increase complexity
        @Override
        public void visit(BinaryExpr n, Void arg) {
            if (n.getOperator() == BinaryExpr.Operator.AND || n.getOperator() == BinaryExpr.Operator.OR) {
                complexity++;
            }
            super.visit(n, arg);
        }

        public int getComplexity() {
            return complexity;
        }
    }
}
