package com.dtstack.jfilebeat.plugin;

public class SgLoader {

    private static ExtLoader JarClassLoader = new ExtLoader();

    public SgLoader(){

    }

    protected static Class<?> getPluginClass(String type,String pluginType,String className) throws ClassNotFoundException{

        String[] names = type.split("\\.");
        String key = String.format("%s:%s",pluginType, names[names.length-1].toLowerCase());
        return JarClassLoader.getClassLoaderByPluginName(key).loadClass(className);

    }


}
