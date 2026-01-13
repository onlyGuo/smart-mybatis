package ink.icoding.smartmybatis.example.service.impl;

import ink.icoding.smartmybatis.entity.expression.Where;
import ink.icoding.smartmybatis.example.entity.Student;
import ink.icoding.smartmybatis.example.enums.Sex;
import ink.icoding.smartmybatis.example.mapper.ChinaCitiesMapper;
import ink.icoding.smartmybatis.example.mapper.StudentMapper;
import ink.icoding.smartmybatis.example.service.StudentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Resource
    private ChinaCitiesMapper chinaCitiesMapper;

    @Override
    public List<Student> searchStudent(String name, Integer minAge, Integer maxAge, Sex sex) {
        return studentMapper.select(Where.where()
                .ifAnd(Student::getName).like(name)
                .ifAnd(Student::getAge).greaterThan(minAge)
                .ifAnd(Student::getAge).lessThan(maxAge)
                .ifAnd(Student::getSex).equalsFor(sex)
        );
    }

    @Override
    public void insertStudent(Student student) {
        studentMapper.insert(student);
    }

    @PostConstruct
    public void test(){

        // Batch insert test data
        String[] names = {"张无忌", "张三丰", "赵敏", "周芷若", "殷素素", "小昭", "谢逊", "成昆", "韦一笑", "杨逍"};
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Student student = new Student();
            student.setName(names[i]);
            student.setAge(18 + i);
            student.setSex(i % 2 == 0 ? Sex.MALE : Sex.FEMALE);
            student.setHobbies(Arrays.asList("唱", "跳", "Rap", "篮球"));
            students.add(student);
        }
        int i1 = studentMapper.insertBatch(students);
        System.out.println("Inserted students: " + students + ", Number of rows inserted: " + i1);

        // Search test
        List<Student> result1 = searchStudent("张", 18, 24, Sex.FEMALE);
        System.out.println("Search results: " + result1);

        // Insert test
        Student newStudent = new Student();
        newStudent.setName("测试用户");
        newStudent.setAge(20);
        newStudent.setSex(Sex.MALE);
        newStudent.setHobbies(Arrays.asList("唱", "跳", "Rap", "篮球"));
        insertStudent(newStudent);
        System.out.println("Inserted new student: " + newStudent);

        // Verify insertion
        List<Student> result2 = searchStudent("测试用户", null, null, null);
        System.out.println("Verification search results: " + result2);

        // Update test
        if (!result2.isEmpty()) {
            Student toUpdate = result2.get(0);
            toUpdate.setAge(21);
            int rowsUpdated = studentMapper.updateById(toUpdate);
            System.out.println("Updated student: " + toUpdate + ", Number of rows updated: " + rowsUpdated);
        }

        // Final verification
        Student result3 = studentMapper.selectById(result2.get(0).getId());
        System.out.println("Final verification search results: " + result3);

        // Delete test
        if (result3 != null) {
            int rowsDeleted = studentMapper.deleteById(result3.getId());
            System.out.println("Deleted student: " + result3 + ", Number of rows deleted: " + rowsDeleted);
        }

        // Final check to ensure deletion
        Student result4 = studentMapper.selectById(result2.get(0).getId());
        System.out.println("Final check after deletion, should be null: " + result4);

        // Delete By Where test
        int rowsDeletedByWhere = studentMapper.delete(Where.where(Student::getName).like("张%"));
        System.out.println("Deleted by where, number of rows deleted: " + rowsDeletedByWhere);

        // Final check to ensure deletion by where
        List<Student> result5 = searchStudent("张", null, null, null);
        System.out.println("Final check after deletion by where, should be empty: " + result5);

        // Count test
        long count = studentMapper.count(Where.where()
                .and(Student::getAge).greaterThan(20)
        );
        System.out.println("Count of students older than 20: " + count);

        // Total Count test
        long totalCount = studentMapper.count();
        System.out.println("Total count of students: " + totalCount);

        // Select All test
        List<Student> allStudents = studentMapper.selectAll();
        System.out.println("All students: " + allStudents);

        // Select By IDs test
        List<Integer> ids = Arrays.asList(1, 2, 3);
        List<Student> studentsByIds = studentMapper.select(Where.where(Student::getId).in(ids));
        System.out.println("Students by IDs " + ids + ": " + studentsByIds);

        // Execute custom SQL test
        int i = studentMapper.executeSql("UPDATE SM_STUDENT SET age = age + 1 WHERE age < ?", 25);
        System.out.println("Custom SQL executed, number of rows affected: " + i);
        List<Student> updatedStudents = studentMapper.select(Where.where(Student::getAge).lessThan(25));
        System.out.println("Students with age less than 25 after custom SQL update: " + updatedStudents);

        // Execute custom SQL with result test
        List<Map<String, Object>> maps = studentMapper
                .queryBySql("SELECT * FROM SM_STUDENT WHERE age >= ? AND sex = ?", 20, Sex.FEMALE);
        System.out.println("Custom SQL query results for age >= 20 and sex = FEMALE: " + maps);

        // Clear test data
        studentMapper.executeSql("TRUNCATE TABLE SM_STUDENT");

        // test init china cities data
        System.out.println(chinaCitiesMapper.selectAll());
    }
}
