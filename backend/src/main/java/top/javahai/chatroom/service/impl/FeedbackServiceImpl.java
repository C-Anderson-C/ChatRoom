package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.dao.FeedbackDao;
import top.javahai.chatroom.entity.Feedback;
import top.javahai.chatroom.service.FeedbackService;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackDao feedbackDao;



    @Override
    public Integer addFeedback(Feedback feedback) {
        return feedbackDao.insertFeedback(feedback);
    }
}
