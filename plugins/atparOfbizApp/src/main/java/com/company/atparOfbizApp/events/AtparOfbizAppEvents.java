package com.company.atparOfbizApp.events;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.DispatchContext;
import java.io.*;
import java.nio.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class AtparOfbizAppEvents {

    public static final String module = AtparOfbizAppEvents.class.getName();

    public static Map<String, Object> createAtparTechieUploadEvent(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer  fileBytes =  (ByteBuffer ) context.get("upload_file");
        String filename = ( String) context.get("uploadFile");
        String filePath = filename;

        byte[] bytefile =fileBytes.array();
        File file2=new File(filePath);
        try {
            File file1 = bytesToFile(bytefile, filePath);
            file2=addTextWatermark("atpar",file1,file2);
            bytefile = Files.readAllBytes(file2.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileName=filename;
        // Specify the file path where the file will be saved
        String destination = "C:/Users/chara/AppData/Local/Temp";

        Path path = Paths.get(destination+"/"+fileName) ;
        try {
            Files.createFile(path);
        } catch (IOException e) {
            System.err.println("already exists: " + e.getMessage());
        }


        // Convert byte array to File
        try {
            File filepath = new File(destination+"/"+fileName);
            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(bytefile);
            fos.close();
            System.out.println("File saved successfully at: " + destination+"/"+fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }

        try {
            Debug.logInfo("=======Creating AtparTechieUpload record in event using service createAtparTechieUpload=========", module);
            Map<String, Object> resultMap=dispatcher.runSync("createAtparTechieUpload", UtilMisc.toMap("uploadFile",filename,"userLogin", userLogin ));
            if (resultMap == null) {
                throw new GenericServiceException("Service [createAtparTechieUpload] did not return a Map object");
            }
            return resultMap;
        } catch (GenericServiceException e) {
            String errMsg = "Unable to create new records in AtparTechieUpload entity: " + e.toString();
            return Collections.emptyMap();
        }
    }

    static File addTextWatermark(String text, File sourceImageFile, File destinationFile) {
        try {
            BufferedImage sourceImage = ImageIO.read(sourceImageFile);
            Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

            // initializes necessary graphic properties
            AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
            g2d.setComposite(alphaChannel);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 256));
            FontMetrics fontMetrics = g2d.getFontMetrics();
            Rectangle2D rect = fontMetrics.getStringBounds(text, g2d);

            // calculates the coordinate where the String is painted
            int centerX = (sourceImage.getWidth() - (int) rect.getWidth()) / 2;
            int centerY = sourceImage.getHeight() / 2;

            // paints the textual watermark
            g2d.drawString(text, centerX, centerY);

            ImageIO.write(sourceImage, "png", destinationFile);
            g2d.dispose();

            System.out.println("The tex watermark is added to the image.");
            return destinationFile;
        } catch (IOException ex) {
            System.err.println(ex);
            return sourceImageFile;
        }
    }
    public static File bytesToFile(byte[] bytes, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
        return file;
    }
}
