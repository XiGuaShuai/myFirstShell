package com.jiami.pack.util;

import com.android.apksigner.ApkSignerTool;
import com.iyxan23.zipalignjava.ZipAlign;
import com.jiami.pack.Const;
import com.jiami.pack.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ApkUtils {

    /**
     * 保存ApplicationName
     * @param apkOutDir
     */
    public static void saveApplicationName(String apkOutDir){
        String androidManifestFile = getManifestFilePath(apkOutDir);
        File appNameOutFile = new File(getOutAssetsDir(apkOutDir),"app_name");
        String appName = ManifestUtils.getApplicationName(androidManifestFile);

        appName = appName == null ? "" : appName;

        IoUtils.writeFile(appNameOutFile.getAbsolutePath(),appName.getBytes());
    }
    /**
     * 获取AndroidManiest.xml路径
     * @param apkOutDir
     * @return
     */
    private static String getManifestFilePath(String apkOutDir){
        return apkOutDir + File.separator + "AndroidManifest.xml";
    }
    /**
     * 获取工作目录下的Assets目录
     * @return
     */
    public static File getOutAssetsDir(String filePath){
        return FileUtils.getDir(filePath,"assets");
    }

    /**
     * 写入代理ApplicationName
     */
    public static void writeProxyAppName(String filePath){
        String inManifestPath = filePath + File.separator + "AndroidManifest.xml";
        String outManifestPath = filePath + File.separator + "AndroidManifest_new.xml";
        ManifestUtils.writeApplicationName(inManifestPath,outManifestPath, Const.PROXY_APPLICATION_NAME);

        File inManifestFile = new File(inManifestPath);
        File outManifestFile = new File(outManifestPath);

        inManifestFile.delete();

        outManifestFile.renameTo(inManifestFile);
    }

    // 压缩dex文件
    public static void compressDexFiles(String apkDir){
        ZipUtils.compress(getDexFiles(apkDir),getOutAssetsDir(apkDir).getAbsolutePath()+File.separator + "dexList");
    }
    /**
     * 获取某个目录下的所有dex文件
     * @param dir
     * @return
     */
    public static List<File> getDexFiles(String dir){
        List<File> dexFiles = new ArrayList<>();
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles();
        if(files != null) {
            Arrays.stream(files).filter(it -> it.getName().endsWith(".dex")).forEach(dexFiles::add);
        }
        return dexFiles;
    }
    // 删除所有的dex文件
    public static void deleteAllDexFiles(String dir){
        List<File> dexFiles = getDexFiles(dir);
        for (File dexFile : dexFiles) {
            dexFile.delete();
        }
    }
    //刪除.renamed文件
    public static void deleteAllRenamedFiles(String dir){
        List<File> dexFiles = getRenamedFiles(dir);
        for (File dexFile : dexFiles) {
            dexFile.delete();
        }
    }
    /**
     * 获取某个目录下的所有.renamed文件
     * @param dir
     * @return
     */
    public static List<File> getRenamedFiles(String dir){
        List<File> dexFiles = new ArrayList<>();
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles();
        if(files != null) {
            Arrays.stream(files).filter(it -> it.getName().endsWith(".renamed1")).forEach(dexFiles::add);
        }
        return dexFiles;

    }
    /**
     * 移动dex到assets目录下
     * @param apkDir
     */
    public static void moveDexToAssets(String apkDir){
        List<File> dexFiles = getDexFiles(apkDir);
        for (File dexFile : dexFiles) {
            File targetFile = new File(getOutAssetsDir(apkDir),dexFile.getName());
            dexFile.renameTo(targetFile);
        }
    }

    /**
     * 添加代理dex
     * @param apkDir
     */
    public static void addProxyDex(String apkDir){
        String proxyDexPath = apkDir+File.separator+"ke/dex/classes.dex";
        addDex(proxyDexPath,apkDir);
    }
    /**
     * 添加dex
     * @param dexFilePath
     * @param apkDir
     */
    public static void addDex(String dexFilePath,String apkDir){
        File dexFile = new File(dexFilePath);
        List<File> dexFiles = getDexFiles(apkDir+File.separator+"ke/dex");
        int newDexNameNumber = dexFiles.size() + 1;
        String newDexPath = apkDir+File.separator + "tmp"+File.separator + "classes.dex";
//        if(newDexNameNumber > 1) {
//            newDexPath = apkDir +File.separator + "tmp"+ File.separator + String.format(Locale.US, "classes%d.dex", newDexNameNumber);
//        }
        newDexPath = apkDir +File.separator + "tmp"+ File.separator + String.format(Locale.US, "classes.dex");
        byte[] dexData = IoUtils.readFile(dexFile.getAbsolutePath());
        IoUtils.writeFile(newDexPath,dexData);
    }

    // 重新打包apk
    public static void buildApk(String originApkPath,String unpackFilePath,String savePath) {

        String originApkName = new File(originApkPath).getName();
        String apkLastProcessDir = ApkUtils.getLastProcessDir().getAbsolutePath();

        String unzipalignApkPath = savePath + File.separator + getUnzipalignApkName(originApkName);
        ZipUtils.compressToApk(unpackFilePath, unzipalignApkPath);

        String keyStoreFilePath = apkLastProcessDir + File.separator + "debug.keystore";

        String keyStoreAssetPath = "E:\\code\\AndroidCode\\codeDpt\\dtp\\assets\\debug.keystore";

        try {
            ZipUtils.readResourceFromRuntime(keyStoreAssetPath, keyStoreFilePath);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        String unsignedApkPath = savePath + File.separator + getUnsignApkName(originApkName);
        boolean zipalignSuccess = false;
        try {
            zipalignApk(unzipalignApkPath, unsignedApkPath);
            zipalignSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        String willSignApkPath = null;
        if (zipalignSuccess) {
            LogUtils.info("zipalign success.");
            willSignApkPath = unsignedApkPath;

        }
        else{
            LogUtils.error("warning: zipalign failed!");
            willSignApkPath = unzipalignApkPath;
        }

        boolean signResult = false;

        String signedApkPath = savePath + File.separator + getSignedApkName(originApkName);


        if(Global.optionSignApk) {
            signResult = signApkDebug(willSignApkPath, keyStoreFilePath, signedApkPath);
        }

        File willSignApkFile = new File(willSignApkPath);
        File signedApkFile = new File(signedApkPath);
        File keyStoreFile = new File(keyStoreFilePath);
        File idsigFile = new File(signedApkPath + ".idsig");


        LogUtils.info("willSignApkFile: %s ,exists: %s",willSignApkFile.getAbsolutePath(),willSignApkFile.exists());
        LogUtils.info("signedApkFile: %s ,exists: %s",signedApkFile.getAbsolutePath(),signedApkFile.exists());

        String resultPath = signedApkFile.getAbsolutePath();
        if (!signedApkFile.exists() || !signResult) {
            resultPath = willSignApkFile.getAbsolutePath();
        }
        else{
            if(willSignApkFile.exists()){
                willSignApkFile.delete();
            }
        }

        if(zipalignSuccess) {
            File unzipalignApkFile = new File(unzipalignApkPath);
            try {
                Path filePath = Paths.get(unzipalignApkFile.getAbsolutePath());
                Files.deleteIfExists(filePath);
            }catch (Exception e){
                LogUtils.debug("unzipalignApkPath err = %s", e);
            }
        }

        if (idsigFile.exists()) {
            idsigFile.delete();
        }

        if (keyStoreFile.exists()) {
            keyStoreFile.delete();
        }
        LogUtils.info("protected apk output path: " + resultPath + "\n");
    }
    /**
     * 获取最后处理（对齐，签名）目录
     * @return
     */
    public static File getLastProcessDir(){
        return FileUtils.getDir(Const.ROOT_OF_OUT_DIR,"dptLastProcess");
    }

    public static String getUnzipalignApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"unzipalign");
    }

    /**
     * 得到一个新的apk文件名
     * @param apkName
     * @return
     */
    public static String getUnsignApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"unsign");
    }

    private static void zipalignApk(String inputApkPath, String outputApkPath) throws Exception{
        RandomAccessFile in = new RandomAccessFile(inputApkPath, "r");
        FileOutputStream out = new FileOutputStream(outputApkPath);
        ZipAlign.alignZip(in, out);
        IoUtils.close(in);
        IoUtils.close(out);
    }

    public static String getSignedApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"signed");
    }

    private static boolean signApkDebug(String apkPath, String keyStorePath, String signedApkPath) {
        if (signApk(apkPath, keyStorePath, signedApkPath,
                Const.KEY_ALIAS,
                Const.STORE_PASSWORD,
                Const.KEY_PASSWORD)) {
            return true;
        }
        return false;
    }
    private static boolean signApk(String apkPath, String keyStorePath, String signedApkPath,
                                   String keyAlias,
                                   String storePassword,
                                   String KeyPassword) {
        ArrayList<String> commandList = new ArrayList<>();

        commandList.add("sign");
        commandList.add("--ks");
        commandList.add(keyStorePath);
        commandList.add("--ks-key-alias");
        commandList.add(keyAlias);
        commandList.add("--ks-pass");
        commandList.add("pass:" + storePassword);
        commandList.add("--key-pass");
        commandList.add("pass:" + KeyPassword);
        commandList.add("--out");
        commandList.add(signedApkPath);
        commandList.add("--v1-signing-enabled");
        commandList.add("true");
        commandList.add("--v2-signing-enabled");
        commandList.add("true");
        commandList.add("--v3-signing-enabled");
        commandList.add("true");
        commandList.add(apkPath);

        int size = commandList.size();
        String[] commandArray = new String[size];
        commandArray = commandList.toArray(commandArray);

        try {
            ApkSignerTool.main(commandArray);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
