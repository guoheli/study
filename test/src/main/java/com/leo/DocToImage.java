package com.leo;

import com.aspose.pdf.devices.PngDevice;
import com.aspose.pdf.devices.Resolution;
import com.aspose.words.*;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DocToImage {

    @Test
    public void toPdf() throws Exception {
        String path = "D:\\Users\\Documents";
        File dirFile = new File(path);
        File[] files = dirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith("~")) {
                    return false;
                }
                if (pathname.getName().endsWith(".doc") || pathname.getName().endsWith(".docx")) {
                    return true;
                }
                return false;
            }
        });
        getWordLicense();
        for (File file : files) {
            // Load the Word document from disk
            String name = file.getName();
            String fileName = name.substring(0, name.lastIndexOf("."));
            InputStream inputStream = new FileInputStream(file);
            Document doc = new Document( inputStream);
            String parent = file.getParent();
            String fileSuffix = parent + "/" + fileName;
            // Save as PDF
            String pdfFile = fileSuffix + ".pdf";
            doc.save(pdfFile);
            pdfToImage(pdfFile, fileSuffix);
            // single add water text
            Font font = new Font("微软雅黑", Font.PLAIN, 60);
            String srcImgPath = fileSuffix + ".png"; //源图片地址
            String tarImgPath = fileSuffix + "_source.png"; //待存储的地址
            Color color = Color.BLUE;

            addWaterMark(srcImgPath, tarImgPath, fileName, color, font);
            // 删除多余的文件
            new File(srcImgPath).delete();
            new File(pdfFile).delete();
        }

    }

    public void zzYK() {
        // {@link zzYk
    }

    public void pdfToImage(String pdfFile, String targetImagePath) throws Exception {
        // For complete examples and data files, please go to https://github.com/aspose-pdf/Aspose.Pdf-for-Java
// Open document
        com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(pdfFile);

// Loop through all the pages of PDF file
        int pageSize = pdfDocument.getPages().size();
        for (int pageCount = 1; pageCount <= pageSize; pageCount++) {
            // Create stream object to save the output image
            java.io.OutputStream imageStream = new java.io.FileOutputStream(targetImagePath + (pageSize == 1 ? ".png" :  pageCount + ".png"));

            // Create Resolution object
            Resolution resolution = new Resolution(300);
            // Create PngDevice object with particular resolution
            PngDevice pngDevice = new PngDevice(resolution);
            // Convert a particular page and save the image to stream
            pngDevice.process(pdfDocument.getPages().get_Item(pageCount), imageStream);

            // Close the stream
            imageStream.close();
        }
    }

    public static void main(String[] args) throws Exception {
        String dir = "D:\\Users\\Documents";
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().startsWith("~")) {
                    return false;
                }
                if (pathname.getName().endsWith(".doc") || pathname.getName().endsWith(".docx")) {
                    return true;
                }
                return false;
            }
        });
        getWordLicense();
        for (File file : files) {
            parseFileToBase64_PNG(file);
        }
    }

    // 将word 转化为图片一张
    public static String parseFileToBase64_PNG(File file) throws Exception {

        //文件流
        InputStream inputStream = new FileInputStream(file);
        //文件 获取文件名字
        String name = file.getName();
        //截取不带后缀名的字段
        String fileName = name.substring(0, name.lastIndexOf("."));

        //文件上传路径
        String parent = file.getParent();

        List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();
        BufferedImage image = null;
        bufferedImages = wordToImg(inputStream);
        image = MergeImage.mergeImage(false, bufferedImages);
        String fullName = parent + "/" + fileName;
        System.out.println();
        boolean png = ImageIO.write(image, "jpg", new File(fullName + "_nwd.jpg"));// 写入流中
        System.out.println(fullName);

        if (png == false) {
            return "转换失败";
        }

        //关闭流
        inputStream.close();

        Font font = new Font("微软雅黑", Font.PLAIN, 35);                     //水印字体
        String srcImgPath = fullName + "_nwd.jpg"; //源图片地址
        String tarImgPath = fullName + ".jpg"; //待存储的地址
        Color color = Color.BLUE;

        addWaterMark(srcImgPath, tarImgPath, fileName, color, font);
        new File(srcImgPath).delete();
        return "转换成功";
    }

    /**
     * @param srcImgPath       源图片路径
     * @param tarImgPath       保存的图片路径
     * @param waterMarkContent 水印内容
     * @param markContentColor 水印颜色
     * @param font             水印字体
     */
    public static void addWaterMark(String srcImgPath, String tarImgPath, String waterMarkContent, Color markContentColor, Font font) {

        try {
            // 读取原图片信息
            File srcImgFile = new File(srcImgPath);//得到文件
            Image srcImg = ImageIO.read(srcImgFile);//文件转化为图片
            int srcImgWidth = srcImg.getWidth(null);//获取图片的宽
            int srcImgHeight = srcImg.getHeight(null);//获取图片的高
            // 加水印
            BufferedImage bufImg = new BufferedImage(srcImgWidth, srcImgHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bufImg.createGraphics();
            g.drawImage(srcImg, 0, 0, srcImgWidth, srcImgHeight, null);
            g.setColor(markContentColor); //根据图片的背景设置水印颜色
            g.setFont(font);              //设置字体

            //设置水印的坐标
            int x = srcImgWidth - 6 * getWatermarkLength(waterMarkContent, g);
            int y = srcImgHeight - 2 * getWatermarkLength(waterMarkContent, g);
            g.drawString(waterMarkContent, x, y);  //画出水印
            g.dispose();
            // 输出图片
            FileOutputStream outImgStream = new FileOutputStream(tarImgPath);
            ImageIO.write(bufImg, "png", outImgStream);
            System.out.println("添加" + waterMarkContent + "水印完成");
            outImgStream.flush();
            outImgStream.close();

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static int getWatermarkLength(String waterMarkContent, Graphics2D g) {
        return g.getFontMetrics(g.getFont()).charsWidth(waterMarkContent.toCharArray(), 0, waterMarkContent.length());
    }

    /**
     * @Description: word和txt文件转换图片
     */
    private static List<BufferedImage> wordToImg(InputStream inputStream) throws Exception {
        try {

            Document doc = new Document(inputStream);
            ImageSaveOptions options = new ImageSaveOptions(SaveFormat.JPEG);
            options.setPrettyFormat(true);
            options.setJpegQuality(100);
            options.setUseAntiAliasing(true);
            options.setUseHighQualityRendering(true);

            int pageCount = doc.getPageCount();

            List<BufferedImage> imageList = new ArrayList<BufferedImage>();
            for (int i = 0; i < pageCount; i++) {
                OutputStream output = new ByteArrayOutputStream();
//                options.setPageIndex(i); // 20.1


                doc.save(output, options);
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(parse(output));
                imageList.add(ImageIO.read(imageInputStream));
            }
            return imageList;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static boolean getWordLicense() {
        boolean result = false;
        try {
            // 凭证
            String licenseStr = "<License><Data><Products><Product>Aspose.Total for Java</Product><Product>Aspose.Words for Java</Product></Products><EditionType>Enterprise</EditionType><SubscriptionExpiry>20991231</SubscriptionExpiry><LicenseExpiry>20991231</LicenseExpiry><SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber></Data><Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature></License>";
            InputStream license = new ByteArrayInputStream(
                    licenseStr.getBytes("UTF-8"));
            License asposeLic = new License();
            asposeLic.setLicense(license);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // outputStream转inputStream
    public static ByteArrayInputStream parse(OutputStream out) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos = (ByteArrayOutputStream) out;
        ByteArrayInputStream swapStream = new ByteArrayInputStream(baos.toByteArray());
        return swapStream;
    }


    public static class MergeImage {

        /**
         * 合并任数量的图片成一张图片
         *
         * @param isHorizontal true代表水平合并，fasle代表垂直合并
         * @param imgs         待合并的图片数组
         * @return
         * @throws IOException
         */
        public static BufferedImage mergeImage(boolean isHorizontal, List<BufferedImage> imgs) throws IOException {
            // 生成新图片
            BufferedImage destImage = null;
            // 计算新图片的长和高
            int allw = 0, allh = 0, allwMax = 0, allhMax = 0;
            // 获取总长、总宽、最长、最宽
            for (int i = 0; i < imgs.size(); i++) {
                BufferedImage img = imgs.get(i);
                allw += img.getWidth();

                if (imgs.size() != i + 1) {
                    allh += img.getHeight() + 2;
                } else {
                    allh += img.getHeight();
                }

                if (img.getWidth() > allwMax) {
                    allwMax = img.getWidth();
                }

                if (img.getHeight() > allhMax) {
                    allhMax = img.getHeight();
                }
            }

            // 创建新图片
            if (isHorizontal) {
                destImage = new BufferedImage(allw, allhMax, BufferedImage.TYPE_INT_RGB);
            } else {
                destImage = new BufferedImage(allwMax, allh, BufferedImage.TYPE_INT_RGB);
            }

            // 注释，分隔线从灰色变成纯黑
            // Graphics2D g2 = (Graphics2D) destImage.getGraphics();
            // g2.setBackground(Color.LIGHT_GRAY);
            // g2.clearRect(0, 0, allw, allh);
            // g2.setPaint(Color.RED);

            // 合并所有子图片到新图片
            int wx = 0, wy = 0;
            for (int i = 0; i < imgs.size(); i++) {
                BufferedImage img = imgs.get(i);
                int w1 = img.getWidth();
                int h1 = img.getHeight();
                // 从图片中读取RGB
                int[] ImageArrayOne = new int[w1 * h1];
                ImageArrayOne = img.getRGB(0, 0, w1, h1, ImageArrayOne, 0, w1); // 逐行扫描图像中各个像素的RGB到数组中
                if (isHorizontal) { // 水平方向合并
                    destImage.setRGB(wx, 0, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
                } else { // 垂直方向合并
                    destImage.setRGB(0, wy, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
                }

                wx += w1;
                wy += h1 + 2;
            }
            return destImage;
        }
    }
}
