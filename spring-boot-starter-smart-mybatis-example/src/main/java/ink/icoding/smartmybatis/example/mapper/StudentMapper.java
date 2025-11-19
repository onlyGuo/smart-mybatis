package ink.icoding.smartmybatis.example.mapper;

import ink.icoding.smartmybatis.example.entity.Student;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Student Mapper 接口
 * @author gsk
 */
@Mapper
public interface StudentMapper extends SmartMapper<Student> {
}
