package ink.icoding.smartmybatis.utils.file;

import org.springframework.core.io.ClassPathResource;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 文件工具类
 * @author gsk
 */
public class FileUtil {

    /**
     * 读取文件内容，返回字符串
     * @param str
     *      文件
     * @return 文件内容
     */
    public static String readFile(File str) {
        ByteArrayOutputStream out = read(str);
        try {
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取文件内容，返回字节流
     * @param str
     *      文件
     * @return 文件内容
     */
    private static ByteArrayOutputStream read(File str) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(str)){
            for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
                out.write(buffer, 0, i);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return out;
    }

    /**
     * 读取文件内容，使用回调函数处理每次读取的字节数组
     * @param str
     *      文件
     * @param handler
     *      读取处理回调函数
     */
    public static void readFile(File str, FileReaderHandler handler) {
        byte[] buffer = new byte[4096];
        try (FileInputStream in = new FileInputStream(str)){
            for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
                handler.read(Arrays.copyOf(buffer, i));
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取输入流内容，使用回调函数处理每次读取的字节数组
     * @param in
     *      输入流
     * @param handler
     *      读取处理回调函数
     */
    public static void readStream(InputStream in, FileReaderHandler handler) throws IOException{
        byte[] buffer = new byte[4096];
        for (int i = in.read(buffer); i > 0; i = in.read(buffer)){
            handler.read(Arrays.copyOf(buffer, i));
        }
    }

    /**
     * 读取文件内容，返回字节数组
     * @param file
     *      文件
     * @return 文件内容
     */
    public static byte[] readFile2Byte(File file){
        ByteArrayOutputStream out = read(file);
        return out.toByteArray();
    }

    /**
     * 写入文件
     * @param bytes
     *      文件内容
     * @param file
     *      目标文件
     */
    public static void writeFile(byte[] bytes, File file) {
        if (!file.getParentFile().exists()){
            if (!file.getParentFile().mkdirs()){
                throw new RuntimeException("目录[" + file.getParentFile() + "]创建失败");
            }
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 复制文件
     * @param source
     *      源文件
     * @param target
     *      目标文件
     */
    public static void copy(File source, File target) {
        if (!target.getParentFile().exists()){
            if (!target.getParentFile().mkdirs()){
                throw new RuntimeException("目录[" + target.getParentFile() + "]创建失败");
            }
            if (target.isDirectory() && !target.exists()){
                if (!target.mkdirs()){
                    throw new RuntimeException("目录[" + target + "]创建失败");
                }
            }
        }
        try (OutputStream out = Files.newOutputStream(target.toPath())){
            readFile(source, out::write);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 将项目资源文件夹下的所有资源文件写入到目标文件夹
     * @param path
     *      资源文件夹路径
     * @param dest
     *      目标文件夹
     */
    public static void writeFolder(String path, File dest) {
        // 先获得资源文件
        List<String> projectResources = getProjectResources(path);
        for (String p: projectResources){
            writeFile(readResource(p), new File(dest, p.split(path)[1]));
        }
    }

    /**
     * 获得自身项目指定路径下的所有资源文件
     * @param path
     *      项目资源路径
     * @return 资源文件列表
     */
    public static List<String> getProjectResources(String path){
        List<String> result = new LinkedList<>();
        Enumeration<URL> resources;
        try {
            resources = Thread.currentThread().getContextClassLoader().getResources(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (resources.hasMoreElements()){
            URL url = resources.nextElement();
            //大概是jar
            String protocol = url.getProtocol();
            if ("jar".equalsIgnoreCase(protocol)) {
                //转换为JarURLConnection
                try {
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    if (connection != null) {
                        JarFile jarFile = connection.getJarFile();
                        if (jarFile != null) {
                            //得到该jar文件下面的类实体
                            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                            while (jarEntryEnumeration.hasMoreElements()) {
                                JarEntry entry = jarEntryEnumeration.nextElement();
                                String jarEntryName = entry.getName();
                                //这里我们需要过滤不是class文件和不在basePack包名下的类
                                if (jarEntryName.startsWith(path) &&
                                        !jarEntryName.startsWith(path + "/.idea") &&
                                        !jarEntryName.endsWith("/")){
                                    result.add(jarEntryName);
                                }
                            }
                        }
                    }
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
            }else if("file".equalsIgnoreCase(protocol)){
                //从maven子项目中扫描
                File file = new File(url.getFile());
                result = getFolderResources(file, path);
            }
        }
        return result;
    }

    /**
     * 递归获取文件夹下的所有资源文件
     * @param file
     *      文件夹
     * @param split
     *      截断路径
     * @return 资源文件列表
     */
    private static List<String> getFolderResources(File file, String split){
        LinkedList<String> result = new LinkedList<>();
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null) {
                for (File sf: files){
                    result.addAll(getFolderResources(sf, split));
                }
            }
        }else{
            String fileName = file.getAbsolutePath();
            if (fileName.contains(split)){
                fileName = split + fileName.split(split)[1];
            }
            result.add(fileName);
        }
        return result;
    }

    /**
     * 将输入流内容写入到文件
     * @param inputStream
     *      输入流
     * @param source
     *      目标文件
     */
    public static void writeStream(InputStream inputStream, File source) {
        if (!source.getParentFile().exists()){
            if (!source.getParentFile().mkdirs()){
                throw new RuntimeException("目录[" + source.getParentFile() + "]创建失败");
            }
        }
        try (FileOutputStream out = new FileOutputStream(source)){
            readStream(inputStream, out::write);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 合并多张图片为一张图片，垂直方向合并
     * @param bufferedImages
     *      图片列表
     * @return 合并后的图片
     */
    public static BufferedImage mergeImage(List<BufferedImage> bufferedImages) {
        int height = 0;
        int width = 0;
        for (BufferedImage bufferedImage: bufferedImages){
            height += bufferedImage.getHeight();
            width = Math.max(width, bufferedImage.getWidth());
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int y = 0;
        for (BufferedImage bufferedImage: bufferedImages){
            image.createGraphics().drawImage(bufferedImage, 0, y, null);
            y += bufferedImage.getHeight();
        }
        return image;
    }

    /**
     * 判断项目资源文件是否存在
     * @param initScriptResourcePath
     *      资源文件路径
     * @return 是否存在
     */
    public static boolean existsResource(String initScriptResourcePath) {
        ClassPathResource classPathResource = new ClassPathResource(initScriptResourcePath);
        return classPathResource.exists();
    }

    /**
     * 文件读取处理回调函数接口
     */
    public interface FileReaderHandler{
        void read(byte[] bytes) throws IOException;
    }

    /**
     * 复制一个目录及其子目录、文件到另外一个目录
     * @param src
     *      源目录
     * @param dest
     *      目标目录
     */
    public static void copyFolder(File src, File dest){
        if (src.isDirectory()) {
            if (!dest.getParentFile().exists()) {
                if (!dest.getParentFile().mkdirs()) {
                    throw new RuntimeException("目录[" + dest.getParentFile() + "]创建失败");
                }
            }
            if (!dest.exists()) {
                if (!dest.mkdirs()) {
                    throw new RuntimeException("目录[" + dest + "]创建失败");
                }
            }
            String[] files = src.list();
            if (null == files){
                return;
            }
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // 递归复制
                copyFolder(srcFile, destFile);
            }
        } else {
            System.out.println("write: " + dest.getAbsolutePath());
            try (InputStream in = Files.newInputStream(src.toPath());
                 OutputStream out = Files.newOutputStream(dest.toPath())){
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 压缩文件或目录为zip文件
     * @param source
     *      源文件或目录
     * @param target
     *      目标zip文件
     */
    public static void zip(File source, File target){
        if (!target.getParentFile().exists()){
            if (!target.getParentFile().mkdirs()){
                throw new RuntimeException("文件[" + source.getParentFile() + "]创建失败");
            }
        }
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))){
            if (source.isDirectory()){
                for (File file: Objects.requireNonNull(source.listFiles())){
                    zip(file, out, file.getName());
                }
            }else{
                zip(source, out, null);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 解压zip输入流到目标目录
     * @param stream
     *      zip输入流
     * @param target
     *      目标目录
     */
    public static void unzip(InputStream stream, File target){
        if (!target.exists()){
            if (!target.mkdirs()){
                throw new RuntimeException("文件[" + target + "]创建失败");
            }
        }
        try (ZipInputStream in = new ZipInputStream(stream)){
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()){
                if (entry.isDirectory()){
                    File file = new File(target, entry.getName());
                    if (!file.exists()){
                        if (!file.mkdirs()){
                            throw new RuntimeException("目录[" + file + "]创建失败");
                        }
                    }
                }else{
                    try(FileOutputStream out = new FileOutputStream(new File(target, entry.getName()))) {
                        readStream(in, out::write);
                    }
                }
                in.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解压zip文件到目标目录
     * @param source
     *      zip文件
     * @param target
     *      目标目录
     */
    public static void unzip(File source, File target){
        try {
            unzip(new FileInputStream(source), target);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 递归压缩文件或目录
     */
    private static void zip(File source, ZipOutputStream out, String name) throws IOException{
        if (null == name){
            name = source.getName();
        }
        if (source.isDirectory()){
            out.putNextEntry(new ZipEntry(name + "/"));
            for (File file: Objects.requireNonNull(source.listFiles())){
                zip(file, out, name + "/" + file.getName());
            }
        }else{
            out.putNextEntry(new ZipEntry(name));
            readFile(source, out::write);
        }
    }

    /**
     * 读取项目资源文件内容，返回字节数组
     * @param path
     *      资源文件路径
     * @return 资源文件内容
     */
    public static byte[] readResource(String path){
        ClassPathResource classPathResource = new ClassPathResource(path);
        byte[] bs = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(InputStream in = classPathResource.getInputStream()){
            for (int i = in.read(bs); i > 0; i = in.read(bs)){
                out.write(bs, 0, i);
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    /**
     * 删除文件夹下的所有文件及子文件夹
     * @param parentFile
     *      目标文件夹
     */
    public static void deleteAll(File parentFile) {
        File[] files = parentFile.listFiles();
        if (null != files){
            for (File file: files){
                if (file.isDirectory()) {
                    deleteAll(file);
                }
                if (!file.delete()){
                    throw new RuntimeException("Clean File Error: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 删除文件或文件夹
     * @param file
     *      目标文件或文件夹
     */
    public static void rm(File file) {
        deleteAll(file);
        if (file.exists()){
            if (!file.delete()){
                throw new RuntimeException("Clean File Error: " + file.getAbsolutePath());
            }
        }
    }
}
