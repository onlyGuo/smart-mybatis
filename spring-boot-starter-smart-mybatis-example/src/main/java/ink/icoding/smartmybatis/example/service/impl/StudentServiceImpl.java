package ink.icoding.smartmybatis.example.service.impl;

import ink.icoding.smartmybatis.entity.expression.C;
import ink.icoding.smartmybatis.entity.expression.ComparisonExpression;
import ink.icoding.smartmybatis.entity.expression.Link;
import ink.icoding.smartmybatis.entity.expression.Where;
import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.example.entity.Student;
import ink.icoding.smartmybatis.example.enums.Sex;
import ink.icoding.smartmybatis.example.mapper.StudentMapper;
import ink.icoding.smartmybatis.example.service.StudentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Student 服务实现类
 * @author gsk
 */
@Service
public class StudentServiceImpl implements StudentService {

    @Resource
    private StudentMapper studentMapper;

    @Override
    public List<Student> searchStudent(String name, Integer minAge, Integer maxAge, Sex sex) {
        return studentMapper.select(Where.where()
                .ifAnd(Student::getName).like(name)
                .ifAnd(Student::getAge).greaterThanOrEquals(minAge)
                .ifAnd(Student::getAge).lessThanOrEquals(maxAge)
                .ifAnd(Student::getSex).equalsFor(sex)
        );
    }

    @Override
    public void insertStudent(Student student) {
        int insert = studentMapper.insert(student);
        System.out.println("Number of rows inserted: " + insert);
    }

    @PostConstruct
    public void test(){
//        Student student = new Student();
//        student.setName("张三");
//        student.setAge(20);
//        student.setSex(Sex.FEMALE);
//        insertStudent(student);
//        System.out.println("Inserted student: " + student);
        List<Student> students = searchStudent("张", 18, 24, Sex.FEMALE);
        System.out.println(students);
    }
}
