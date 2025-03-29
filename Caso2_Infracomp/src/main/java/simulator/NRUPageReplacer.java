package main.java.simulator;
import java.util.*;

public class NRUPageReplacer implements PageReplacer {
    @Override
    public int selectPageToReplace(Map<Integer, MemoryManager.PageTableEntry> pageTable, 
                                 List<Integer> physicalFrames,
                                 BitSet referenceBits) {
        // Clasificar páginas en las 4 clases NRU
        List<List<Integer>> classes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            classes.add(new ArrayList<>());
        }
        
        // Clasificar cada página en un marco físico
        for (int frame : physicalFrames) {
            for (Map.Entry<Integer, MemoryManager.PageTableEntry> entry : pageTable.entrySet()) {
                if (entry.getValue().frameNumber == frame) {
                    int page = entry.getKey();
                    boolean referenced = referenceBits.get(page);
                    boolean modified = entry.getValue().modified;
                    
                    // Determinar clase NRU (0: !R!M, 1: !RM, 2: R!M, 3: RM)
                    int classNum = (!referenced ? 0 : 2) + (!modified ? 0 : 1);
                    classes.get(classNum).add(page);
                    break;
                }
            }
        }
        
        // Seleccionar la primera página de la clase no vacía de menor número
        for (int i = 0; i < 4; i++) {
            if (!classes.get(i).isEmpty()) {
                // Seleccionar la primera página (no aleatoria para consistencia)
                return classes.get(i).get(0);
            }
        }
        
        // Fallback: si todas las clases están vacías (no debería pasar)
        return physicalFrames.get(0);
    }
}

interface PageReplacer {
    int selectPageToReplace(Map<Integer, MemoryManager.PageTableEntry> pageTable,
                          List<Integer> physicalFrames,
                          BitSet referenceBits);
}
