package ds.android.tasknet.task;

import java.io.Serializable;

import ds.android.tasknet.config.Node;

public class TaskAdvReply implements Serializable {

    private static final long serialVersionUID = 1L;
    private String TaskAdvReplyId;
    private String taskId;
    private Node node;
    private float loadCanServe;

    public TaskAdvReply(String TaskAdvReplyId, String taskId, Node node, float loadCanServe) {
        super();
        this.TaskAdvReplyId = TaskAdvReplyId;
        this.node = node;
        this.taskId = taskId;
        this.loadCanServe = loadCanServe;
    }

    public String getTaskAdvReplyId() {
        return TaskAdvReplyId;
    }

    public void setTaskAdvReplyId(String taskAdvReplyId) {
        TaskAdvReplyId = taskAdvReplyId;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public float getLoadCanServe() {
        return loadCanServe;
    }

    public void setLoadCanServe(float loadCanServe) {
        this.loadCanServe = loadCanServe;
    }

    public String toString() {
        return "--------------------------------------"
                + "\nAdvertisement ID: " + TaskAdvReplyId
                + "\nTask ID: " + taskId
                + "\n" + node
                + "Load: " + loadCanServe
                + "\n--------------------------------------";
    }
}
