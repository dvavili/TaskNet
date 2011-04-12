package ds.android.tasknet.task;

import java.io.Serializable;

import ds.android.tasknet.config.Node;
import ds.android.tasknet.config.Preferences;

public class TaskChunk implements Serializable {

	private static final long serialVersionUID = 1L;
	String taskId;
    Enum<Preferences.TASK_CHUNK_STATUS> status;
    Integer sequenceNumber;
    Node node;
    DistributedTask dsTask;
    String taskAdvReplyId;
    int retry;
    
	public TaskChunk(String taskId, Node node, Integer sequenceNumber, 
			DistributedTask dsTask, String taskAdvReplyId) {
		super();
		this.taskId = taskId;
		this.node = node;
		this.sequenceNumber = sequenceNumber;		
		this.dsTask = dsTask;
		this.status = Preferences.TASK_CHUNK_STATUS.DISTRIBUTED;
		this.taskAdvReplyId = taskAdvReplyId;
		retry = 0;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public Enum<Preferences.TASK_CHUNK_STATUS> getStatus() {
		return status;
	}

	public void setStatus(Enum<Preferences.TASK_CHUNK_STATUS> status) {
		this.status = status;
	}

	public Integer getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public DistributedTask getDsTask() {
		return dsTask;
	}

	public void setDsTask(DistributedTask dsTask) {
		this.dsTask = dsTask;
	}

	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}
	
	public void incrRetry() {
		this.retry++;
	}

	public String getTaskAdvReplyId() {
		return taskAdvReplyId;
	}

	public void setTaskAdvReplyId(String taskAdvReplyId) {
		this.taskAdvReplyId = taskAdvReplyId;
	}
		           
	
}
