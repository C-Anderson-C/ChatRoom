package top.javahai.chatroom.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import top.javahai.chatroom.entity.Report;

@Mapper
public interface ReportDao {

    Integer insertReport(Report report);
}