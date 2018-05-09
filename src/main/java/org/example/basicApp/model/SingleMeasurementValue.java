/*
*
* Developed by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.model;

public class SingleMeasurementValue {
	
	private String measurementName;
	private Float value;
	private String postfix;
	
	public SingleMeasurementValue (String name, Float value, String postfix) {		
		this.measurementName = name;
		this.value = value;
		this.postfix = postfix;		
	}
	
	public String getMeasurementName() {		
		return measurementName;
	}
	
	public void setMeasurementName(String name) {		
		this.measurementName = name;
	}	
	
	public Float getValue() {		
		return value;
	}
	
	public void setValue (Float value) {		
		this.value = value;
	}
	
	public String getPostFix() {		
		return postfix;
	}
	
	public void setPostFix (String postifx) {		
		this.postfix = postfix;
	}
		
   @Override
    public String toString() {
        return String.format("%s %.3f %s \n",measurementName, value, postfix);
    }
}
