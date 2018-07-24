package com.dtstack.jfilebeat.plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dtstack.jfilebeat.common.logger.Log;


public class ExtLoader {

    private static Log logger = Log.getLogger(ExtLoader.class);

    public final String PLUGIN_PATH = "/plugin";

    public final String NATIVE_PLUGIN_PATH = "";

    private static String userDir = System.getProperty("user.dir");

    private Map<String,URL[]> jarUrls = null;

    public ExtLoader(){
        if(jarUrls==null){
            jarUrls = getClassLoadJarUrls();
            if(jarUrls != null&&jarUrls.size() > 0){
                Thread.currentThread().setContextClassLoader(null);
            }
        }
    }

    public ClassLoader getClassLoaderByPluginName(String name){
        URL[] urls =  jarUrls.get(name);
        ClassLoader classLoader = this.getClass().getClassLoader();
        if(urls==null || urls.length==0){
            logger.warn("{}:load by app classLoader",name);
            return classLoader;
        }
        return new URLClassLoader(urls,classLoader);
    }

    private Map<String,URL[]> getClassLoadJarUrls(){
        Map<String,URL[]>  result  = new ConcurrentHashMap<String, URL[]>();
        try{
            logger.warn("userDir:{}",userDir);
            String plugin = String.format("%s/plugin/", userDir);
            File pluginJar = new File(plugin);
            if(!pluginJar.exists()){
                throw new ExtException(String.format("%s folder not found", plugin));
            }

            Map<String,URL[]>  inputs = getClassLoadJarUrls(pluginJar);
            result.putAll(inputs);
            logger.warn("getClassLoadJarUrls:{}",result);
        }catch(Exception e){
            logger.error("getClassLoadJarUrls error:{}",ExceptionUtil.getErrorMessage(e));
        }
        return result;
    }

    private Map<String,URL[]> getClassLoadJarUrls(File dir) throws MalformedURLException, IOException{
        String dirName = dir.getName();
        Map<String,URL[]> urls = new ConcurrentHashMap<String, URL[]>();
        File[] files = dir.listFiles();
        if (files!=null&&files.length>0){
            for(File f:files){
                String jarName = f.getName();
                if(f.isFile()&&jarName.endsWith(".jar")){
                    jarName = jarName.split("-")[0].toLowerCase();
                    String[] jns = jarName.split("\\.");
                    urls.put(String.format("%s:%s",dirName,jns.length==0?jarName:jns[jns.length-1]), new URL[]{f.toURI().toURL()});
                }
            }
        }
        return urls;
    }

}
