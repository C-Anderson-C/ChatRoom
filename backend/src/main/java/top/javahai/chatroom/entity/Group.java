package top.javahai.chatroom.entity;

import java.util.Date;

public class Group {
    private Integer id;
    private String name;
    private String description;
    private Integer creatorId;
    private Date createTime;

    // 可选的非持久化字段：是否为当前组
    private boolean isCurrentGroup;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public boolean isCurrentGroup() {
        return isCurrentGroup;
    }

    public void setCurrentGroup(boolean currentGroup) {
        isCurrentGroup = currentGroup;
    }
}