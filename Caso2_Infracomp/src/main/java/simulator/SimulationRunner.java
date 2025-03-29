package main.java.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class SimulationRunner {
    private final int numFrames;
    private final String referenceFilePath;
    private MemoryManager memoryManager;
    private int pageSize;
    private int totalReferences;
    private int virtualPages;
    
    // Constantes de tiempo de acceso definidas correctamente
    private static final long RAM_ACCESS_TIME_NS = 50;        // 50 nanosegundos
    private static final long PAGE_FAULT_TIME_NS = 10_000_000; // 10 milisegundos en nanosegundos
    
    public SimulationRunner(int numFrames, String referenceFilePath) {
        this.numFrames = numFrames;
        this.referenceFilePath = referenceFilePath;
    }
    
    public void runSimulation() throws IOException {
        // Leer parámetros del archivo de referencias
        try (BufferedReader reader = new BufferedReader(new FileReader(referenceFilePath))) {
            // Leer encabezado
            String headerLine = reader.readLine();
            pageSize = Integer.parseInt(headerLine.split("=")[1]);
            
            reader.readLine(); // NF
            reader.readLine(); // NC
            String nrLine = reader.readLine(); // NR
            totalReferences = Integer.parseInt(nrLine.split("=")[1]);
            
            String npLine = reader.readLine(); // NP
            virtualPages = Integer.parseInt(npLine.split("=")[1]);
            
            // Inicializar memory manager con las constantes de tiempo
            memoryManager = new MemoryManager(pageSize, numFrames, virtualPages, 
                                           RAM_ACCESS_TIME_NS, PAGE_FAULT_TIME_NS);
            
            // Configurar hilos
            ExecutorService executor = Executors.newFixedThreadPool(2);
            ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor();
            
            // Hilo para actualización periódica de bits de referencia (cada 1ms)
            clock.scheduleAtFixedRate(() -> memoryManager.resetReferenceBits(), 
                                    1, 1, TimeUnit.MILLISECONDS);
            
            // Hilo para procesar referencias
            executor.execute(() -> {
                try {
                    int refCount = 0;
                    String currentLine;
                    while ((currentLine = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                        String[] parts = currentLine.split(",");
                        if (parts.length < 4) continue;
                        
                        int pageNumber = Integer.parseInt(parts[1]);
                        boolean isWrite = parts[3].equals("W");
                        
                        memoryManager.processMemoryAccess(pageNumber, isWrite);
                        
                        refCount++;
                        if (refCount % 10000 == 0) {
                            try {
                                Thread.sleep(1); // Esperar 1ms cada 10,000 referencias
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            // Esperar a que terminen los hilos
            executor.shutdown();
            clock.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                clock.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void printResults() {
        System.out.println("\n=== Resultados de la Simulación ===");
        System.out.println("Configuración:");
        System.out.printf("- Tamaño de página: %d bytes\n", pageSize);
        System.out.printf("- Marcos asignados: %d\n", numFrames);
        System.out.printf("- Páginas virtuales: %d\n", virtualPages);
        System.out.printf("- Total referencias: %d\n", totalReferences);
        
        System.out.println("\nEstadísticas de Paginación:");
        System.out.printf("- Hits de página: %d (%.2f%%)\n", 
                         memoryManager.getPageHits(), 
                         memoryManager.getHitPercentage());
        System.out.printf("- Fallos de página: %d (%.2f%%)\n", 
                         memoryManager.getPageFaults(),
                         memoryManager.getMissPercentage());
        
        System.out.println("\nTiempos de Acceso:");
        System.out.printf("- Tiempo total con paginación: %.3f ms\n", 
                         memoryManager.getTotalAccessTime() / 1_000_000.0);
        System.out.printf("- Tiempo si todo estuviera en RAM: %.3f ms\n", 
                         (totalReferences * RAM_ACCESS_TIME_NS) / 1_000_000.0);
        System.out.printf("- Tiempo si todo generara fallos: %.3f ms\n", 
                         (totalReferences * PAGE_FAULT_TIME_NS) / 1_000_000.0);
        
        System.out.println("\nAnálisis de Rendimiento:");
        double slowdown = (double) memoryManager.getTotalAccessTime() / 
                         (totalReferences * RAM_ACCESS_TIME_NS);
        System.out.printf("- Slowdown por paginación: %.2fx\n", slowdown);
        System.out.printf("- Eficiencia de memoria: %.2f%%\n", 
                         (double) numFrames / virtualPages * 100);
    }
}