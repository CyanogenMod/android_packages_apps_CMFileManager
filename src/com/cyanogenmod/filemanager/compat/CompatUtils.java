package com.cyanogenmod.filemanager.compat;

import java.lang.reflect.Field;

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
    
}
