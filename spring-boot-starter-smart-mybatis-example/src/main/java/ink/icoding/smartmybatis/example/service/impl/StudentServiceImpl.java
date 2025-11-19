package ink.icoding.smartmybatis.example.service.impl;

import ink.icoding.smartmybatis.entity.expression.C;
import ink.icoding.smartmybatis.entity.expression.Where;
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
//        return studentMapper.select(Where.where()
//                .and(Student::getName, C.LIKE, name)
//                .and(Student::getAge, C.GTE, minAge)
//                .and(Student::getAge, C.LTE, maxAge)
//                .and(Student::getSex, C.EQ, sex)
//        );
        System.out.println(studentMapper.count());
        return studentMapper.select(Where.where()
                .and(Student::getName).like(name)
                .and(Student::getAge).greaterThan(minAge)
                .and(Student::getAge).lessThan(maxAge)
                .and(Student::getSex).equalsFor(sex));
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
