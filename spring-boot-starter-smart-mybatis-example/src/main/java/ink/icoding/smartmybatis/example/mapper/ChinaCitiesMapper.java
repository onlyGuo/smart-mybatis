package ink.icoding.smartmybatis.example.mapper;

import ink.icoding.smartmybatis.example.entity.ChinaCities;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChinaCitiesMapper extends SmartMapper<ChinaCities> {
}
