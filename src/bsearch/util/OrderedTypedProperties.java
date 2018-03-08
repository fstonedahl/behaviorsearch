package bsearch.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.moeaframework.util.TypedProperties;

public class OrderedTypedProperties {

	public LinkedHashMap<String,String> props = new LinkedHashMap<>();
	
	public OrderedTypedProperties() {	
	}
	
	public void putAll(Map<? extends String,? extends String> map) {
		props.putAll(map);
	}
	
	public LinkedHashMap<String,String> asOrderedMap() {
		LinkedHashMap<String,String> retVal = new LinkedHashMap<>();
		retVal.putAll(props);
		return retVal;
	}
	public Properties asProperties() {
		Properties regularProps = new Properties();
		regularProps.putAll(props);
		return regularProps;
	}
	
//	public TypedProperties asTypedProperties() {
//		TypedProperties tProps = new TypedProperties();
//		tProps.getProperties().putAll(props);
//		return tProps;
//	}
	
	public void setString(String key, String value) {
		props.put(key,value);
	}
	public void setFloat(String key, float value) {
		setString(key, Float.toString(value));
	}
	public void setDouble(String key, double value) {
		setString(key, Double.toString(value));
	}
	public void setByte(String key, byte value) {
		setString(key, Byte.toString(value));
	}
	public void setShort(String key, short value) {
		setString(key, Short.toString(value));
	}
	public void setInt(String key, int value) {
		setString(key, Integer.toString(value));
	}
	public void setLong(String key, long value) {
		setString(key, Long.toString(value));
	}
	public void setBoolean(String key, boolean value) {
		setString(key, Boolean.toString(value));
	}
	public void setStringArray(String key, String[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setStringArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	
	public void setFloatArray(String key, float[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setFloatArray(key, values);
		setString(key, tProps.getString(key, null));
	}

	public void setDoubleArray(String key, double[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setDoubleArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	public void setByteArray(String key, byte[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setByteArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	public void setShortArray(String key, short[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setShortArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	public void setIntArray(String key, int[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setIntArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	public void setLongArray(String key, long[] values) {
		TypedProperties tProps = new TypedProperties();
		tProps.setLongArray(key, values);
		setString(key, tProps.getString(key, null));
	}
	
	
	
}
