package com.jiami.pack.util;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;


/** 结合了其他的utils类
 * @author luoyesiqiu
 */
public class FileUtils {
    /**
     * 复制一个文件或目录到其他位置
     * @param src
     * @param dest
     */
    public static void copy(String src,String dest){
        File srcFile = new File(src);
        File destFile = new File(dest);
        Queue<File> queue = new LinkedList<>();
        queue.offer(srcFile);
        while(!queue.isEmpty()) {
            File origin = queue.poll();
            File target = null;
            if (origin.isDirectory()) {
                target = new File(destFile, origin.getName());
                File[] subOrigin = origin.listFiles();
                if(subOrigin != null) {
                    for (File f : subOrigin) {
                        queue.offer(f);
                    }
                }
                if (!target.exists()) {
                    target.mkdirs();
                }
            } else {

                target = new File(dest,origin.getParentFile().getName());
                File targetFile = new File(target,origin.getName());
                byte[] data = IoUtils.readFile(origin.getAbsolutePath());
                IoUtils.writeFile(targetFile.getAbsolutePath(),data);
            }
        }
    }

    /**
     * 得到一个新文件的路径
     * @param fileName
     * @param tag
     * @return
     */
    public static String getNewFileName(String fileName,String tag){
        String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        return fileName.replaceAll("\\." + fileSuffix + "$","_" + tag + "." + fileSuffix) ;
    }

    public static String getNewFileSuffix(String fileName,String newSuffix){
        String fileSuffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        return fileName.replaceAll("\\." + fileSuffix + "$",  "." + newSuffix) ;
    }

    /**
     * 得到一个目录，不存在则创建
     * @param path 父路径
     * @param dirName 目录
     * @return
     */
    public static File getDir(String path,String dirName){
        File dirFile = new File(path,dirName);
        if(!dirFile.exists()){
            dirFile.mkdirs();
        }
        return dirFile;
    }

    /**
     * 递归删除目录/文件
     * @param file
     */
    public static void deleteRecurse(File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files != null){
                for (File f : files) {
                    deleteRecurse(f);
                }
            }
        }
        file.delete();
    }

    /**
     * 获取当前命令工具所在的目录
     */
    public static String getExecutablePath(){
        return System.getProperty("user.dir");
    }
}
