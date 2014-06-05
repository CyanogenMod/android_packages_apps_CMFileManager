package com.cyanogenmod.filemanager.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;



import android.content.res.Resources;

public class CompatUtils {
	
    public static boolean getInternalBoolean(Resources res, String key) {
        boolean result = false;
        int resourceId = res.getIdentifier(key, "bool", "android");
        if (resourceId > 0) {
            result = res.getBoolean(resourceId);
        }
        return result;
    }
    
    public static String getInternalString(Resources res, String key) {
    	String result = null;
    	int resourceId = res.getIdentifier(key, "string", "android");
    	if (resourceId > 0) {
    		result = res.getString(resourceId);
    	}
    	return result;
    }
    
    public static int getClsHideIntStaticField(Class<?> cls, String identifier, int defaultValue){
    	try {
			Field field = cls.getDeclaredField(identifier);
			return field.getInt(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return defaultValue;
    }
    
    public static int getClsHideIntStaticMethod(Class<?> cls, String identifier, int defaultValue){
    	try {
			Method method = cls.getDeclaredMethod(identifier);
			return (Integer) method.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return defaultValue;
    }
    
    public static boolean getClsHideBooleanStaticMethod(Class<?> cls, String identifier, boolean defaultValue){
    	try {
			Method method = cls.getDeclaredMethod(identifier);
			return (Boolean) method.invoke(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return defaultValue;
    }
    
    public static Object getClsHideObjectStaticMethod(Class<?> cls, String identifier, Object... args){
    	try {
    		if(args != null){
    			int len = args.length;
    			Class<?>[] clss = new Class<?>[len];
    			for(int i=0; i<len; i++){
    				clss[i] = args[i].getClass();
    			}
    			Method method = cls.getDeclaredMethod(identifier, clss);
    			return method.invoke(null, args);
    		}else{
    			Method method = cls.getDeclaredMethod(identifier);
    			return method.invoke(null, args);
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return null;
    }
    
}
