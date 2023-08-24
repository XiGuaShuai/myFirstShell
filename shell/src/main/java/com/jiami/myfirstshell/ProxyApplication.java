package com.jiami.myfirstshell;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends android.app.Application{
    private static final String TAG = "xigua";
    private static final String dexZip = "classes.dex";
    private static Context ApplicationContext;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ApplicationContext =base;
        loadDex();


    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void loadDex() {
        Log.e(TAG, "DummyApplication.attachBaseContext: 开始加载");
        //1. 拷贝自定义资源（assets/src.dex）的dex到程序目录下(app/files/)
        String dexPath = copyDex(dexZip);
        //2. 创建一个加载了该Dex文件的Dex类加载器, 可以反射获取其中的类和函数
        DexClassLoader dexClassLoader = getLoader(dexPath);
        //3. 替换PathClassLoader为自定义classLoader, startActivity 底层会从PathClassLoader寻找加载的类
        replaceClassLoader(dexClassLoader);
        Log.e(TAG, "DummyApplication.attachBaseContext: 加载结束");
        // 加载activity
/*        try {
            Class activityClass= dexClassLoader.loadClass("com.cxhifuewnoiew.myapplication.MainActivity");

            // 以这种方式启动activity
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(new Intent(this,activityClass));
            stackBuilder.startActivities();
            // startActivity(new Intent(StuApplication.this,activityClass));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }*/

    }
    // 拷贝assets/文件 到 app/files/ 下
    private String copyDex(String dexName) {
        // 获取assets目录管理器
        AssetManager as = getAssets();
        // 目的地址
        String path = getFilesDir() + File.separator + dexName;
        // /data/user/0/com.example.myapplication/files/Main2Activity.dex
        Log.e(TAG, "copyDex: path: " + path);
        // 将文件拷贝到目的地址
        try {
            // 创建文件流
            FileOutputStream out = new FileOutputStream(path);
            // 打开文件 assets/dexName
            InputStream is = as.open(dexName);
            // 循环读取并写入文件
            byte[] buffer = new byte[1024];
            int len =0;
            while((len = is.read(buffer))!= -1){
                out.write(buffer, 0, len);
            }
            // 关闭文件
            out.close();
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
        return path;
    }
    // 创建一个加载了该Dex文件的Dex类加载器, 可以反射获取其中的类和函数
    private DexClassLoader getLoader(String dexPath) {
        DexClassLoader dexClassLoader = new DexClassLoader(
                dexPath,            // 要加载的Dex文件路径
                getCacheDir().toString(),   // 优化之后的文件路径
                ApplicationContext.getApplicationInfo().nativeLibraryDir,     // native 库路径， 由于没有使用到native，null即可
                getClassLoader()        // 父ClassLoader
        );
        Log.e(TAG, "getLoader: "+dexClassLoader);
        return dexClassLoader;
    }
    private void replaceClassLoader(DexClassLoader dexClassLoader) {
        try {
            //1. private static ActivityThread sCurrentActivityThread = ActivityThread.currentActivityThread()
            // 获取类类型
            Class clzActivityThread = Class.forName("android.app.ActivityThread");
            // 获取静态方法
            Method methodCurrentActivityThread = clzActivityThread.getDeclaredMethod("currentActivityThread");
            // 调用静态方法, 返回静态成员变量
            Object sCurrentActivityThread = methodCurrentActivityThread.invoke(null, new Object[]{});
            //2. private ActivityThread$AppBindData mBoundApplication = sCurrentActivityThread 反射 mBoundApplication
            // 获取类类型
            Class clzAppBinData = Class.forName("android.app.ActivityThread$AppBindData");
            // 获取字段类型
            Field fieldBoundApplication = clzActivityThread.getDeclaredField("mBoundApplication");
            // 获取实例sCurrentActivityThread 对应的字段对象
            fieldBoundApplication.setAccessible(true);
            Object mBoundApplication = fieldBoundApplication.get(sCurrentActivityThread);
            //3. private LoadedApk info = mBoundApplication 反射 info
            // 获取字段类类型
            Class clzLoadedApk = Class.forName("android.app.LoadedApk");
            // 获取字段类型
            Field fieldInfo = clzAppBinData.getDeclaredField("info");
            // 获取实例mBoundApplication 对应的字段对象
            fieldInfo.setAccessible(true);
            Object info = fieldInfo.get(mBoundApplication);
            //4. private ClassLoader mClassLoader = info 反射 mClassLoader
            // 获取类类型
            Class clzClassLoader = Class.forName("java.lang.ClassLoader");
            // 获取字段类型
            Field fieldClassLoader = clzLoadedApk.getDeclaredField("mClassLoader");
            // 获取实例mBoundApplication 对应的字段对象
            fieldClassLoader.setAccessible(true);
            fieldClassLoader.set(info, dexClassLoader);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
