package com.jiami.pack;

import com.jiami.pack.util.ApkUtils;
import com.jiami.pack.util.FileUtils;
import com.jiami.pack.util.LogUtils;
import com.jiami.pack.util.ManifestUtils;
import com.jiami.pack.util.ZipUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Pack {
    public static void main(String[] args) {
        // 首先解析参数
        parseOptions(args);
        if(Global.optionApkPath!=null) {
            // 开始对apk进行操作
            processApk(Global.optionApkPath);

        }
    }

    /***
     * 用来解析配置项的
     * @param args main传入进来的参数
     */
    private static void parseOptions(String[] args){
        Options options = new Options();
        //s no-sign false  不签名apk
        options.addOption(new Option(Const.OPTION_NO_SIGN_APK,Const.OPTION_NO_SIGN_APK_LONG,false,"Do not sign apk."));
        //d dump-code false Dex的code item 存到.json文件中
        options.addOption(new Option(Const.OPTION_DUMP_CODE,Const.OPTION_DUMP_CODE_LONG,false,"Dump the code item of DEX and save it to .json files."));
        //l noisy-log false 打开日志
        options.addOption(new Option(Const.OPTION_OPEN_NOISY_LOG,Const.OPTION_OPEN_NOISY_LOG_LONG,false,"Open noisy log."));
        //f apk-file true 需要保护的apk文件
        options.addOption(new Option(Const.OPTION_APK_FILE,Const.OPTION_APK_FILE_LONG,true,"Need to protect apk file."));
        //D debug false 使apk可以调试
        options.addOption(new Option(Const.OPTION_DEBUGGABLE,Const.OPTION_DEBUGGABLE_LONG,false,"Make apk debuggable."));
        //c disable-acf false 禁用应用程序组件工厂（仅用于调试）--翻译的
        options.addOption(new Option(Const.OPTION_DISABLE_APP_COMPONENT_FACTORY,Const.OPTION_DISABLE_APP_COMPONENT_FACTORY_LONG,false,"Disable app component factory(just use for debug)."));

        CommandLineParser commandLineParser = new DefaultParser();
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            if(!commandLine.hasOption(Const.OPTION_APK_FILE)){  // 不包含f参数就命令行提示
                usage(options);
                return;
            }
            //LogUtils.setOpenNoisyLog(commandLine.hasOption(Const.OPTION_OPEN_NOISY_LOG)); //openNoisyLog 判断是否要打开日志
            Global.optionApkPath = commandLine.getOptionValue(Const.OPTION_APK_FILE); // 设置需要保护的apk路径
            Global.dumpCode = commandLine.hasOption(Const.OPTION_DUMP_CODE); // 设置是否需要code item 保存到.json文件中
            Global.optionSignApk = !commandLine.hasOption(Const.OPTION_NO_SIGN_APK); // 前面有个！ 是否需要签名
            Global.debuggable = commandLine.hasOption(Const.OPTION_DEBUGGABLE);  // 编译出来的apk是否可调试
            Global.disabledAppComponentFactory = commandLine.hasOption(Const.OPTION_DISABLE_APP_COMPONENT_FACTORY); // 是否 禁用应用程序组件工厂

        } catch (ParseException e) {
            usage(options);
            throw new RuntimeException(e);
        }

    }
    private static void usage(Options options){
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("java -jar pack.jar [option] -f <apk>",options);
    }

    private static void processApk(String apkPath){
        // 判断是否存在shell

        // 判断apk是否存在
        File apkFile = new File(apkPath);

        if(!apkFile.exists()){
            LogUtils.error("Apk not exits!");
            return; //apk不存在返回
        }

        String apkAbPath  = apkFile.getAbsolutePath();
        // 获取到父级目录
        Path path = Paths.get(apkAbPath);
        Path directoryPath = path.getParent();
        String tmpDir = directoryPath.toString()+ File.separator +"tmp";
        //1.解压apk
        // 获取apk所在的目录
        //String apkMainProcessPath = apkFile.getParent(); // apk文件解压的路径
        LogUtils.info("Apk main process path: " + tmpDir);
        ZipUtils.extractAPK(apkAbPath,tmpDir);
        //获取app的包名
        Global.packageName = ManifestUtils.getPackageName(tmpDir + File.separator + "AndroidManifest.xml");
        //保存原本有的application_name
        //ApkUtils.saveApplicationName(tmpDir);
        //修改代理的application_name
        ApkUtils.writeProxyAppName(tmpDir);
        //压缩dex文件
/*        ApkUtils.compressDexFiles(tmpDir);*/
        // 移动dex到assets目录下
        ApkUtils.moveDexToAssets(tmpDir);
        //刪除dex文件
        ApkUtils.deleteAllDexFiles(tmpDir);
        ApkUtils.deleteAllRenamedFiles(tmpDir);
        // 添加代理的dex，也就是壳的dex
        ApkUtils.addProxyDex(directoryPath.toString());
// 重新打包apk
        ApkUtils.buildApk(apkFile.getAbsolutePath(),tmpDir, FileUtils.getExecutablePath());

        // 删除tmp
        // 删除tmp文件夹下的数据
        File apkMainProcessFile = new File(tmpDir);
        if (apkMainProcessFile.exists()) {
            FileUtils.deleteRecurse(apkMainProcessFile);
        }
        LogUtils.info("All done.");

       // ZipUtils.extractAPK(s,apkMainProcessPath);
    }

}
