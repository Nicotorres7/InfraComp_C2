package main.java.sobel;

import java.io.*;

public class Imagen {
    public byte[] header = new byte[54];
    public byte[][][] imagen;
    public int alto, ancho;
    public int padding;
    private String filePath;
    
    public Imagen(String nombre) {
        this.filePath = nombre;
        try {
            FileInputStream fis = new FileInputStream(nombre);
            fis.read(header);
            
            ancho = ((header[21] & 0xFF) << 24) | ((header[20] & 0xFF) << 16) |
                    ((header[19] & 0xFF) << 8) | (header[18] & 0xFF);
            alto = ((header[25] & 0xFF) << 24) | ((header[24] & 0xFF) << 16) |
                   ((header[23] & 0xFF) << 8) | (header[22] & 0xFF);
            
            imagen = new byte[alto][ancho][3];
            
            int rowSizeSinPadding = ancho * 3;
            padding = (4 - (rowSizeSinPadding % 4)) % 4;
            
            byte[] pixel = new byte[3];
            for (int i = 0; i < alto; i++) {
                for (int j = 0; j < ancho; j++) {
                    fis.read(pixel);
                    imagen[i][j][0] = pixel[0];
                    imagen[i][j][1] = pixel[1];
                    imagen[i][j][2] = pixel[2];
                }
                fis.skip(padding);
            }
            fis.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public void escribirImagen(String output) {
        byte pad = 0;
        try {
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(header);
            byte[] pixel = new byte[3];
            
            for (int i = 0; i < alto; i++) {
                for (int j = 0; j < ancho; j++) {
                    pixel[0] = imagen[i][j][0];
                    pixel[1] = imagen[i][j][1];
                    pixel[2] = imagen[i][j][2];
                    fos.write(pixel);
                }
                for (int k = 0; k < padding; k++) fos.write(pad);
            }
            fos.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    public String getFilePath() {
        return filePath;
    }
}
   
