package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import top.javahai.chatroom.entity.Feedback;

@Mapper
public interface FeedbackDao {

    Integer insertFeedback(Feedback feedback);
}