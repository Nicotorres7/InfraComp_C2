package main.java.simulator;

import main.java.sobel.Imagen;
import main.java.sobel.FiltroSobel;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReferenceGenerator {
    private final int pageSize;
    private String outputFilePath;
    
    // Tamaños fijos de los filtros Sobel (3x3 enteros)
    private static final int SOBEL_FILTER_SIZE = 3;
    private static final int INT_SIZE = 4; // bytes
    
    public ReferenceGenerator(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public void generateReferences(String imagePath) throws IOException {
        // Cargar imagen
        Imagen imagenIn = new Imagen(imagePath);
        Imagen imagenOut = new Imagen(imagePath);
        
        // Calcular organización en memoria
        MemoryLayout layout = calculateMemoryLayout(imagenIn);
        
        // Generar referencias simuladas
        List<String> references = simulateSobelReferences(imagenIn, layout);
        
        // Escribir archivo de salida
        writeReferenceFile(imagenIn, references.size(), layout.totalPages, references);
    }
    
    private MemoryLayout calculateMemoryLayout(Imagen imagen) {
        MemoryLayout layout = new MemoryLayout();
        
        // Tamaño de cada componente en bytes
        layout.imageSize = imagen.alto * imagen.ancho * 3; // 3 bytes por pixel (RGB)
        layout.filterXSize = SOBEL_FILTER_SIZE * SOBEL_FILTER_SIZE * INT_SIZE;
        layout.filterYSize = SOBEL_FILTER_SIZE * SOBEL_FILTER_SIZE * INT_SIZE;
        layout.resultSize = imagen.alto * imagen.ancho * 3; // Mismo tamaño que imagen original
        
        // Calcular páginas necesarias para cada componente
        layout.imageStartPage = 0;
        layout.imagePages = (int) Math.ceil((double) layout.imageSize / pageSize);
        
        layout.filterXStartPage = layout.imageStartPage + layout.imagePages;
        layout.filterXPages = (int) Math.ceil((double) layout.filterXSize / pageSize);
        
        layout.filterYStartPage = layout.filterXStartPage + layout.filterXPages;
        layout.filterYPages = (int) Math.ceil((double) layout.filterYSize / pageSize);
        
        layout.resultStartPage = layout.filterYStartPage + layout.filterYPages;
        layout.resultPages = (int) Math.ceil((double) layout.resultSize / pageSize);
        
        layout.totalPages = layout.resultStartPage + layout.resultPages;
        
        return layout;
    }
    
    private List<String> simulateSobelReferences(Imagen imagen, MemoryLayout layout) {
        List<String> references = new ArrayList<>();
        
        // Simular accesos durante la aplicación del filtro Sobel
        for (int i = 1; i < imagen.alto - 1; i++) {
            for (int j = 1; j < imagen.ancho - 1; j++) {
                // 1. Accesos a la imagen original (lecturas de píxeles 3x3)
                for (int ki = -1; ki <= 1; ki++) {
                    for (int kj = -1; kj <= 1; kj++) {
                        // Calcular dirección de memoria para cada píxel
                        int pixelIndex = ((i + ki) * imagen.ancho + (j + kj)) * 3;
                        addImageReferences(references, i+ki, j+kj, pixelIndex, layout);
                    }
                }
                
                // 2. Accesos a los filtros (lecturas)
                addFilterReferences(references, layout);
                
                // 3. Escritura del resultado
                int resultIndex = (i * imagen.ancho + j) * 3;
                addResultReferences(references, i, j, resultIndex, layout);
            }
        }
        
        return references;
    }
    
    private void addImageReferences(List<String> references, int row, int col, int startIndex, MemoryLayout layout) {
        // Para cada componente RGB del píxel
        for (int c = 0; c < 3; c++) {
            int byteOffset = startIndex + c;
            int pageNumber = layout.imageStartPage + (byteOffset / pageSize);
            int offsetInPage = byteOffset % pageSize;
            
            String component = (c == 0) ? "r" : (c == 1) ? "g" : "b";
            references.add(String.format("Imagen[%d][%d].%s,%d,%d,R", row, col, component, pageNumber, offsetInPage));
        }
    }
    
    private void addFilterReferences(List<String> references, MemoryLayout layout) {
        // Accesos a SOBEL_X (9 elementos, cada uno de 4 bytes)
        for (int ki = 0; ki < 3; ki++) {
            for (int kj = 0; kj < 3; kj++) {
                int elementOffset = (ki * 3 + kj) * INT_SIZE;
                int pageNumber = layout.filterXStartPage + (elementOffset / pageSize);
                int offsetInPage = elementOffset % pageSize;
                references.add(String.format("SOBEL_X[%d][%d],%d,%d,R", ki, kj, pageNumber, offsetInPage));
            }
        }
        
        // Accesos a SOBEL_Y (9 elementos, cada uno de 4 bytes)
        for (int ki = 0; ki < 3; ki++) {
            for (int kj = 0; kj < 3; kj++) {
                int elementOffset = (ki * 3 + kj) * INT_SIZE;
                int pageNumber = layout.filterYStartPage + (elementOffset / pageSize);
                int offsetInPage = elementOffset % pageSize;
                references.add(String.format("SOBEL_Y[%d][%d],%d,%d,R", ki, kj, pageNumber, offsetInPage));
            }
        }
    }
    
    private void addResultReferences(List<String> references, int row, int col, int startIndex, MemoryLayout layout) {
        // Para cada componente RGB del píxel resultante
        for (int c = 0; c < 3; c++) {
            int byteOffset = startIndex + c;
            int pageNumber = layout.resultStartPage + (byteOffset / pageSize);
            int offsetInPage = byteOffset % pageSize;
            
            String component = (c == 0) ? "r" : (c == 1) ? "g" : "b";
            references.add(String.format("Rta[%d][%d].%s,%d,%d,W", row, col, component, pageNumber, offsetInPage));
        }
    }
    
    private void writeReferenceFile(Imagen imagen, int numReferences, int totalPages, List<String> references) 
            throws IOException {
        // Crear nombre de archivo de salida
        String imageName = new File(imagen.getFilePath()).getName().replace(".bmp", "");
        outputFilePath = String.format("%s_ref_%dB.txt", imageName, pageSize);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Escribir encabezado
            writer.println("TP=" + pageSize);
            writer.println("NF=" + imagen.alto);
            writer.println("NC=" + imagen.ancho);
            writer.println("NR=" + numReferences);
            writer.println("NP=" + totalPages);
            
            // Escribir referencias
            for (String ref : references) {
                writer.println(ref);
            }
        }
    }
    
    public String getOutputFilePath() {
        return outputFilePath;
    }
    
    // Clase auxiliar para organizar la información de layout de memoria
    private class MemoryLayout {
        int imageSize;
        int filterXSize;
        int filterYSize;
        int resultSize;
        
        int imageStartPage;
        int imagePages;
        
        int filterXStartPage;
        int filterXPages;
        
        int filterYStartPage;
        int filterYPages;
        
        int resultStartPage;
        int resultPages;
        
        int totalPages;
    }
}