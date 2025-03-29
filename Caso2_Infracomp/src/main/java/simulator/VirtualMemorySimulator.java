package main.java.simulator;

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualMemorySimulator {
    private static final int RAM_ACCESS_TIME = 50; // ns
    private static final int SWAP_ACCESS_TIME = 10_000_000; // 10 ms en ns

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            printMenu();
            int option = getIntInput(scanner, "Seleccione opción: ");
            
            switch (option) {
                case 1:
                    handleOption1(scanner);
                    break;
                case 2:
                    handleOption2(scanner);
                    break;
                case 3:
                    System.out.println("Saliendo del programa...");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Simulador de Memoria Virtual ===");
        System.out.println("1. Generar referencias de página");
        System.out.println("2. Simular comportamiento de memoria");
        System.out.println("3. Salir");
    }

    private static void handleOption1(Scanner scanner) {
        System.out.println("\n=== Generación de Referencias ===");
        int pageSize = getIntInput(scanner, "Ingrese tamaño de página (bytes): ");
        String imagePath = getStringInput(scanner, "Ingrese ruta del archivo BMP: ");
        
        try {
            ReferenceGenerator generator = new ReferenceGenerator(pageSize);
            generator.generateReferences(imagePath);
            System.out.println("Referencias generadas exitosamente en: " + generator.getOutputFilePath());
        } catch (IOException e) {
            System.err.println("Error al generar referencias: " + e.getMessage());
        }
    }

    private static void handleOption2(Scanner scanner) {
        System.out.println("\n=== Simulación de Memoria ===");
        int numFrames = getIntInput(scanner, "Ingrese número de marcos de página: ");
        String refFilePath = getStringInput(scanner, "Ingrese ruta del archivo de referencias: ");
        
        try {
            SimulationRunner runner = new SimulationRunner(numFrames, refFilePath);
            runner.runSimulation();
            runner.printResults();
        } catch (IOException e) {
            System.err.println("Error al ejecutar simulación: " + e.getMessage());
        }
    }

    private static int getIntInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Entrada inválida. Por favor ingrese un número.");
            scanner.next();
            System.out.print(prompt);
        }
        return scanner.nextInt();
    }

    private static String getStringInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.next();
    }
}