package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.dao.ReportDao;
import top.javahai.chatroom.entity.Report;
import top.javahai.chatroom.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportDao reportDao;

    @Override
    public Integer addReport(Report report) {
        return reportDao.insertReport(report);
    }
}