package ink.icoding.smartmybatis.example.service;

import ink.icoding.smartmybatis.example.entity.Student;
import ink.icoding.smartmybatis.example.enums.Sex;

import java.util.List;

/**
 * Student 服务接口
 * @author gsk
 */
public interface StudentService {

    /**
     * 搜索学生
     * @param name 姓名
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @param sex 性别
     * @return 学生列表
     */
    List<Student> searchStudent(String name, Integer minAge, Integer maxAge, Sex sex);


    /**
     * 插入学生
     * @param student 学生实体
     */
    void insertStudent(Student student);
}
