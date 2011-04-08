package ds.android.tasknet.task;

import java.io.Serializable;

public class DistributedTask extends Task implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String className;
	String methodName;
	Serializable[] parameters;
	Integer sequenceNumber;
	
	public DistributedTask(Integer load, String id, String src, String clsName, String method, Serializable[] params) {
		super(load, id, src);
		className = clsName;
		methodName = method;
		parameters = params;
		sequenceNumber = -1;
	}
	
	public String getClassName(){
		return className;
	}
	
	public String getMethodName(){
		return methodName;
	}
	
	public Serializable[] getParameters(){
		return parameters;
	}
	
	public Integer getSeqNumber(){
		return sequenceNumber;
	}
}
