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

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.channels.FileChannel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AtparOfbizAppEvents {

    public static final String module = AtparOfbizAppEvents.class.getName();

    //    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static Map<String, Object> createAtparProductEvent(DispatchContext dctx, Map<String, ?> context) throws IOException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes= null;
        if(context.containsKey("upload_file_file")){
            String base64FileContent = (String) context.get("upload_file_file");
            byte[] bytes = Base64.getDecoder().decode(base64FileContent);
             fileBytes = ByteBuffer.wrap(bytes);
        }
        else if(context.containsKey("upload_file")){
            fileBytes = (ByteBuffer) context.get("upload_file");
        }

//        ByteBuffer fileBytes=fileToByteBuffer(file);

        String filename = (String) context.get("_upload_file_fileName");
        String fileContentType = (String) context.get("_upload_file_contentType");
        String productId = (String) context.get("productId");
        String productTypeId = (String) context.get("productTypeId");
        String internalName = (String) context.get("internalName");
        Integer status = (Integer) context.get("status");
        String longDescription = (String) context.get("longDescription");
        String primaryProductCategoryId = (String) context.get("primaryProductCategoryId");
        String introductionDate = (String) context.get("introductionDate");
//        LocalDateTime  introductionDate = LocalDateTime.parse(String.format((String)context.get("introductionDate"),formatter));

        String thumbnailPath="";
       if(fileBytes!=null) {
           byte[] bytefile = fileBytes.array();
           String destination = System.getProperty("java.io.tmpdir");
           String storePath = storeFileInDir(destination, bytefile, productId, filename);

           File file2 = new File(filename);
           try {
               File file1 = bytesToFile(bytefile, filename);
               file2 = addTextWatermark("atpar", file1, file2);
               ByteBuffer resizedImage = resizeImage(file2, 140, 140);
               bytefile = resizedImage.array();
           } catch (IOException e) {
               e.printStackTrace();
           }
           destination = System.getProperty("user.dir") + File.separator + "themes" + File.separator + "common-theme" + File.separator + "webapp" + File.separator + "images" + File.separator + "PendingProducts" + File.separator + "thumbnailImage";
            thumbnailPath = storeFileInDir(destination, bytefile, productId, filename);
       }
        try {
            Debug.logInfo("=======Creating AtparProduct record in event using service createAtparProduct=========", module);
            Map<String, Object> resultMap = dispatcher.runSync("createAtparProduct", UtilMisc.toMap("userLogin", userLogin, "productId", productId, "atparProductInternalName", internalName, "status", status, "longDescription", longDescription, "atparProductType", productTypeId, "atparProductCategoryId", primaryProductCategoryId, "introductionDate", introductionDate, "thumbnailImagePath", thumbnailPath));
            if (resultMap == null) {
                throw new GenericServiceException("Service [createAtparProduct] did not return a Map object");
            }
            return resultMap;
        } catch (GenericServiceException e) {
            String errMsg = "Unable to create new records in AtparProduct entity: " + e.toString();
            return Collections.emptyMap();
        }
    }

    public static ByteBuffer fileToByteBuffer(File file) throws IOException {
        // Create a FileInputStream to read the file
        try (FileInputStream fis = new FileInputStream(file); FileChannel fileChannel = fis.getChannel()) {
            // Get the size of the file
            long fileSize = fileChannel.size();

            // Create a ByteBuffer to hold the file's content
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileSize);

            // Read the file's content into the ByteBuffer
            fileChannel.read(byteBuffer);

            // Flip the ByteBuffer to prepare it for reading
            byteBuffer.flip();

            return byteBuffer;
        }
    }
    public static Map<String, Object> createAtparTechieUploadEvent(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String atparProductId = (String) context.get("atparProductId");
        String productId = (String) context.get("productId");
        String productTypeId = (String) context.get("atparProductType");
        String internalName = (String) context.get("atparProductInternalName");
        String longDescription = (String) context.get("longDescription");

        String primaryProductCategoryId = (String) context.get("atparProductCategoryId");
        String introductionDate = (String) context.get("introductionDate");
        String filePath = (String) context.get("thumbnailImagePath");


        //  String fileName = path.getFileName().toString();
        // Read all bytes from the file
        Map<String, Object> resultMap = new HashMap<>();

        try {
            Path path = Paths.get(filePath);
            byte[] byteFile = Files.readAllBytes(path);
            // Specify the file path where the file will be save

            String[] fileNames = {"small.png", "medium.png", "large.png", "detail.png", "original.png"};
//         destination = "C:/Users/chara/shivain22/ofbiz-framework/themes/common-theme/webapp/images/products";
            String destination = System.getProperty("user.dir") + File.separator + "themes" + File.separator + "common-theme" + File.separator + "webapp" + File.separator + "images" + File.separator + "products";

            for (String fileNameIn : fileNames) {

                path = Paths.get(destination + File.separator + productId + File.separator + fileNameIn);
                Files.createDirectories(path.getParent());
                Files.createFile(path);

                // Convert byte array to File

                try {
                    File filepath = new File(destination + File.separator + productId + File.separator + fileNameIn);
                    FileOutputStream fos = new FileOutputStream(filepath);
                    fos.write(byteFile);
                    fos.close();
                    System.out.println("File saved successfully at: " + destination + File.separator + productId + File.separator + fileNameIn);
                } catch (IOException e) {
                    e.printStackTrace();
                    resultMap.put("result", "Error");
                    resultMap.put("message", "Error adding Product to Ecommerce Page.");
                    resultMap.put("atparProductId", atparProductId);
                    return resultMap;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            resultMap.put("result", "Error");
            resultMap.put("message", "Product Image File cannot be read.");
            resultMap.put("atparProductId", atparProductId);
            return resultMap;
        }


//        try {
//            Debug.logInfo("=======Creating AtparTechieUpload record in event using service createAtparTechieUpload=========", module);
//            Map<String, Object> resultMap=dispatcher.runSync("createAtparTechieUpload", UtilMisc.toMap("uploadFile",filename,"userLogin", userLogin ));
//            if (resultMap == null) {
//                throw new GenericServiceException("Service [createAtparTechieUpload] did not return a Map object");
//            }
//            return resultMap;
//        } catch (GenericServiceException e) {
//            String errMsg = "Unable to create new records in AtparTechieUpload entity: " + e.toString();
//            return Collections.emptyMap();
//        }
        resultMap.put("result", "success");
        resultMap.put("atparProductId", atparProductId);
        return resultMap;
    }

    public static String storeFileInDir(String destination, byte[] bytefile, String productId, String fileName) {


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
            return destination + productId + File.separator + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
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
        String productId = (String) context.get("productId");
        String filePath = filename;

        byte[] bytefile = fileBytes.array();
        File file2 = new File(filePath);
        Map<String, Object> resultMap = new HashMap<>();

        String destination = System.getProperty("java.io.tmpdir");

        Path path = Paths.get(destination + productId + File.separator + "gallery" + File.separator + filename);

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (IOException e) {
            System.err.println("already exists: " + e.getMessage());
        }


        // Convert byte array to File
        try {
            File filepath = new File(destination + productId + File.separator + "gallery" + File.separator + filename);
            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(bytefile);
            fos.close();
            System.out.println("File saved successfully at: " + destination + productId + File.separator + "gallery" + File.separator + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }


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

    public static Map<String, Object> downloadProductContent(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productId = (String) context.get("productId");

        Map<String, Object> resultMap=new HashMap<>();
        String destination = System.getProperty("user.dir") + File.separator + "themes" + File.separator + "common-theme" + File.separator + "webapp" + File.separator + "images" + File.separator + "products" + File.separator + "management";
        Path sourceDir = Paths.get(destination, productId);
        String finalDestination = System.getProperty("java.io.tmpdir");
        String zipFileName = productId + "contents.zip";
        Path zipFilePath = Paths.get(finalDestination, zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zipFilePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File sourceFile = sourceDir.toFile();
            zipFile(sourceFile, sourceFile.getName(), zos);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        resultMap.put("message", "success");
        return resultMap;
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}