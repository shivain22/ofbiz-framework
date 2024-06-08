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
        String filename = ( String) context.get("_upload_file_fileName");
        String fileContentType = (String) context.get("_upload_file_contentType");
        String productId = ( String) context.get("productId");
        String filePath = filename;

        byte[] bytefile =fileBytes.array();
        File file2=new File(filePath);
        try {
            File file1 = bytesToFile(bytefile, filePath);
            file2=addTextWatermark("atpar",file1,file2);
            bytefile = Files.readAllBytes(file1.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileName=filename;
        // Specify the file path where the file will be saved
        String destination = System.getProperty("java.io.tmpdir");

        Path path = Paths.get(destination + productId + File.separator + fileName);

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (IOException e) {
            System.err.println("already exists: " + e.getMessage());
        }


        // Convert byte array to File
        try {
            File filepath = new File(destination + productId + File.separator + fileName);
            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(bytefile);
            fos.close();
            System.out.println("File saved successfully at: " + destination + productId + File.separator + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }

        String [] fileNames={"small.png","medium.png","large.png","detail.png","original.png"};
//         destination = "C:/Users/chara/shivain22/ofbiz-framework/themes/common-theme/webapp/images/products";
         destination = System.getProperty("user.dir")+File.separator+"themes"+File.separator+"common-theme"+File.separator+"webapp"+File.separator+"images"+File.separator+"products";

        for(String fileNameIn :fileNames){

             path = Paths.get(destination + File.separator + productId + File.separator + fileNameIn) ;

            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (IOException e) {
                System.err.println("already exists: " + e.getMessage());
            }


            // Convert byte array to File
            try {
                File filepath = new File(destination + File.separator + productId + File.separator + fileNameIn);
                FileOutputStream fos = new FileOutputStream(filepath);
                fos.write(bytefile);
                fos.close();
                System.out.println("File saved successfully at: " + destination + File.separator + productId + File.separator + fileNameIn);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
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
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
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

    public static Map<String, Object> updateUploadsizeEvent(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        String filename = (String) context.get("_uploadedFile_fileName");
        String fileContentType = (String) context.get("_uploadedFile_contentType");
        String filePath = filename;

        byte[] bytefile = fileBytes.array();
        File file2 = new File(filePath);
        Map<String, Object> resultMap = new HashMap<>();
        try {
            File file1 = bytesToFile(bytefile, filePath);
            file2 = addTextWatermark("@par", file1, file2);
            ByteBuffer resizedImage = resizeImage(file2, 140, 140);
            resultMap.put("uploadedFile", resizedImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultMap;
    }
    private static ByteBuffer resizeImage(File inputFile, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile);
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, getFileExtension(inputFile.getName()), baos);
        byte[] imageBytes = baos.toByteArray();

        return ByteBuffer.wrap(imageBytes);
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        String[] parts = fileName.split("\\.");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

}
