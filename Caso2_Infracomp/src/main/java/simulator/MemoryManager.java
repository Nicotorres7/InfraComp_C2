package main.java.simulator;

import java.util.*;
import java.util.concurrent.locks.*;

public class MemoryManager {
    private final int pageSize;
    private final int numFrames;
    private final int totalVirtualPages;
    private final PageReplacer pageReplacer;
    private final ReentrantLock lock;
    private final long ramAccessTime;
    private final long pageFaultTime;
    
    // Estructuras de datos compartidas
    private final Map<Integer, PageTableEntry> pageTable;
    private final List<Integer> physicalFrames;
    private final BitSet referenceBits;
    
    // Estadísticas
    private int pageFaults;
    private int pageHits;
    private long totalAccessTime;
    
    public MemoryManager(int pageSize, int numFrames, int totalVirtualPages, 
                       long ramAccessTime, long pageFaultTime) {
        this.pageSize = pageSize;
        this.numFrames = numFrames;
        this.totalVirtualPages = totalVirtualPages;
        this.ramAccessTime = ramAccessTime;
        this.pageFaultTime = pageFaultTime;
        this.pageReplacer = new NRUPageReplacer();
        this.lock = new ReentrantLock();
        
        this.pageTable = new HashMap<>();
        this.physicalFrames = new ArrayList<>(numFrames);
        this.referenceBits = new BitSet(totalVirtualPages);
        
        this.pageFaults = 0;
        this.pageHits = 0;
        this.totalAccessTime = 0;
        
        initializePageTable();
    }
    
    private void initializePageTable() {
        for (int i = 0; i < totalVirtualPages; i++) {
            pageTable.put(i, new PageTableEntry());
        }
    }
    
    public void processMemoryAccess(int pageNumber, boolean isWrite) {
        lock.lock();
        try {
            PageTableEntry entry = pageTable.get(pageNumber);
            
            // Marcar como referenciada
            entry.referenced = true;
            referenceBits.set(pageNumber, true);
            
            if (isWrite) {
                entry.modified = true;
            }
            
            if (entry.isInMemory()) {
                handlePageHit();
            } else {
                handlePageFault(pageNumber, entry);
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void handlePageHit() {
        pageHits++;
        totalAccessTime += ramAccessTime;
    }
    
    private void handlePageFault(int pageNumber, PageTableEntry entry) {
        pageFaults++;
        
        if (physicalFrames.size() >= numFrames) {
            replacePage();
        }
        
        loadPage(pageNumber, entry);
    }
    
    private void replacePage() {
        int pageToReplace = pageReplacer.selectPageToReplace(pageTable, physicalFrames, referenceBits);
        PageTableEntry replacedEntry = pageTable.get(pageToReplace);
        
        // Limpiar entrada de la página reemplazada
        replacedEntry.frameNumber = -1;
        replacedEntry.referenced = false;
        
        // Eliminar marco de la lista física
        physicalFrames.remove(Integer.valueOf(replacedEntry.frameNumber));
    }
    
    private void loadPage(int pageNumber, PageTableEntry entry) {
        // Asignar nuevo marco (simulado)
        int newFrame = findFreeFrame();
        entry.frameNumber = newFrame;
        entry.referenced = true;
        physicalFrames.add(newFrame);
        
        totalAccessTime += pageFaultTime;
    }
    
    private int findFreeFrame() {
        // En un sistema real buscaríamos un marco libre
        // Aquí devolvemos el siguiente número disponible
        return physicalFrames.size();
    }
    
    public void resetReferenceBits() {
        lock.lock();
        try {
            // Resetear todos los bits de referencia periódicamente
            referenceBits.clear();
        } finally {
            lock.unlock();
        }
    }
    
    // Métodos de acceso a resultados
    public int getPageFaults() { return pageFaults; }
    public int getPageHits() { return pageHits; }
    public long getTotalAccessTime() { return totalAccessTime; }
    
    public double getHitPercentage() {
        int total = pageHits + pageFaults;
        return total > 0 ? (double) pageHits / total * 100 : 0;
    }
    
    public double getMissPercentage() {
        int total = pageHits + pageFaults;
        return total > 0 ? (double) pageFaults / total * 100 : 0;
    }
    
    // Clase para entradas de la tabla de páginas
    protected static class PageTableEntry {
        int frameNumber = -1; // -1 indica que no está en memoria
        boolean referenced = false;
        boolean modified = false;
        
        boolean isInMemory() {
            return frameNumber != -1;
        }
    }
}